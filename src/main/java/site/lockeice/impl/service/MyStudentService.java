package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import site.lockeice.impl.util.FullNameCheck;

import javax.annotation.Nullable;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

public class MyStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "insert into students " +
                    "(sid, first_name, last_name, major_id, enrolled_date)" +
                    "values(?, ?, ?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setInt(4, majorId);
            statement.setDate(5, enrolledDate);
            statement.execute();
            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSearchEntry> searchCourse(
            int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName,
            @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
            @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType,
            boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites,
            int pageSize, int pageIndex
    ) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();

            String sql = """
                    select class_id, class_name, capacity,
                                    course_id, course_name, credit, hour, grading, 
                                    left_capacity, prerequisite
                    from (
                        select distinct cls.class_id, cls.class_name, cls.capacity, 
                               crs.course_id, crs.course_name, crs.credit, crs.hour, crs.grading,
                               (cls.capacity - (select count(*) from course_select cs where cs.class_id = cls.class_id)) as 
                               left_capacity,
                               crs.prerequisite
                        from classes cls
                        join courses crs on crs.course_id = cls.course_id
                        join class_timetable ctt on cls.class_id = ctt.class_id
                        join locations l on ctt.location_id = l.location_id
                        join class_teachers ct on ctt.class_timetable_id = ct.class_timetable_id
                        join teachers t on t.user_id = ct.teacher_id
                        join course_type c on cls.course_id = c.course_id
                        where cls.semester_id = ? and
                              (cls.course_id like ? or ?) and
                              (crs.course_name || '[' || cls.class_name || ']' like ? or ?) and
                              (
                                (
                                    (t.first_name || ' ' || t.last_name like ?) or
                                    (t.first_name || t.last_name like ?)
                                ) 
                                or ?
                               ) and
                              (ctt.weekday = ? or ?) and
                              ((ctt.time_begin <= ? and ctt.time_end >= ?) or ?) and
                              (l.location like any(?) or ?) and
                              (c.course_type = ? or ?)
                    ) as subq
                    where (? or left_capacity > 0) and -- capacity check
                          (? or subq.class_id not in (
                                    select course_select.class_id
                                    from course_select
                                    where grade >= 60
                          )) -- passed check
                    order by course_id, (course_name || ' ' || class_name)
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, semesterId);
            statement.setString(2, searchCid == null ? "" : "%" + searchCid + "%");
            statement.setBoolean(3, searchCid == null);
            statement.setString(4, searchName == null ? "" : "%" + searchName + "%");
            statement.setBoolean(5, searchName == null);
            statement.setString(6, searchInstructor == null ? "" : "%" + searchInstructor + "%");
            statement.setString(7, searchInstructor == null ? "" : "%" + searchInstructor + "%");
            statement.setBoolean(8, searchInstructor == null);
            statement.setInt(9, searchDayOfWeek == null ? 0 : searchDayOfWeek.getValue());
            statement.setBoolean(10, searchDayOfWeek == null);
            statement.setInt(11, searchClassTime == null ? 0: searchClassTime);
            statement.setInt(12, searchClassTime == null ? 0: searchClassTime);
            statement.setBoolean(13, searchClassTime == null);
            Array locations = null;
            if (searchClassLocations != null) {
                for (int i = 0; i < searchClassLocations.size(); i++)
                    searchClassLocations.set(i, "%" + searchClassLocations.get(i) + "%");
                locations = conn.createArrayOf("varchar", searchClassLocations.toArray());
            }
            statement.setArray(14, searchClassLocations == null ?
                                                 conn.createArrayOf("varchar", new Object[]{""}) :
                                                 locations);
            statement.setBoolean(15, searchClassLocations == null);
            statement.setInt(16, searchCourseType.ordinal());
            statement.setBoolean(17, searchCourseType == CourseType.ALL);
            statement.setBoolean(18, ignoreFull);
            statement.setBoolean(19, ignorePassed);

            ResultSet res = statement.executeQuery();

            /***
             * REMEMBER TO DELETE!!!
             */
            ignoreMissingPrerequisites = !ignoreMissingPrerequisites;

            ArrayList<CourseSearchEntry> entries = new ArrayList<>();
            int cnt = 1;
            while (res.next()) {
                if (ignoreMissingPrerequisites ||
                        passedPrerequisitesForCourse(studentId, res.getString("course_id"))) {
                    if (cnt > pageSize + pageSize * pageIndex ||
                        cnt <= pageSize * pageIndex) {
                        cnt++;
                        continue;
                    }
                    CourseSearchEntry entry = new CourseSearchEntry();

                    // Course info part
                    entry.course = new Course();
                    entry.course.id = res.getString("course_id");
                    entry.course.name = res.getString("course_name");
                    entry.course.credit = res.getInt("credit");
                    if (res.getString("grading").equals("HM"))
                        entry.course.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                    else if (res.getString("grading").equals("PF"))
                        entry.course.grading = Course.CourseGrading.PASS_OR_FAIL;
                    entry.course.classHour = res.getInt("hour");

                    // Course section info part
                    entry.section = new CourseSection();
                    entry.section.id = res.getInt("class_id");
                    entry.section.name = res.getString("class_name");
                    entry.section.totalCapacity = res.getInt("capacity");
                    entry.section.leftCapacity = res.getInt("left_capacity");

                    // Get section classes by class_id
                    entry.sectionClasses = new HashSet<>();
                    String queryCourseSectionClass = """
                                select distinct (ctt.class_timetable_id),
                                       teacher_id, (first_name || ' ' || last_name) as full_name,
                                       weekday, time_begin, time_end, location
                                from class_timetable ctt
                                join class_teachers ct on ctt.class_timetable_id = ct.class_timetable_id
                                join teachers t on t.user_id = ct.teacher_id
                                join locations l on ctt.location_id = l.location_id
                                where ctt.class_id = ?
                            """;
                    PreparedStatement s = conn.prepareStatement(queryCourseSectionClass);
                    s.setInt(1, entry.section.id);
                    ResultSet sectionClassRes = s.executeQuery();

                    while (sectionClassRes.next()) {
                        CourseSectionClass css = new CourseSectionClass();
                        css.id = sectionClassRes.getInt("class_timetable_id");
                        css.instructor = new Instructor();
                        css.instructor.id = sectionClassRes.getInt("teacher_id");
                        if (FullNameCheck.isChinese(sectionClassRes.getString("full_name").charAt(0)))
                            css.instructor.fullName = sectionClassRes.getString("full_name").replace(" ", "");
                        else
                            css.instructor.fullName = sectionClassRes.getString("full_name");
                        css.dayOfWeek = DayOfWeek.of(sectionClassRes.getInt("weekday"));
                        css.classBegin = sectionClassRes.getShort("time_begin");
                        css.classEnd = sectionClassRes.getShort("time_end");
                        css.location = sectionClassRes.getString("location");

                        // Get week list by ctt_id
                        String queryWeekList = """
                                    select week from class_week_list where class_timetable_id = ?
                                """;
                        PreparedStatement p = conn.prepareStatement(queryWeekList);
                        p.setInt(1, css.id);
                        ResultSet weekListRes = p.executeQuery();
                        css.weekList = new HashSet<>();
                        while (weekListRes.next())
                            css.weekList.add(weekListRes.getShort(1));
                        entry.sectionClasses.add(css);
                    }

                    // Get conflict courses names
                    String queryConflictCourses = """
                                select distinct c.course_name, cls1.class_name
                                from course_select cs
                                -- cls1: conflict, cls2: original
                                join classes cls1 on cls1.class_id = cs.class_id
                                join classes cls2 on cls2.class_id = ?
                                join class_timetable ct1 on cls1.class_id = ct1.class_id
                                join class_timetable ct2 on cls2.class_id = ct2.class_id
                                join courses c on cls1.course_id = c.course_id
                                where (cls1.course_id = cls2.course_id) or -- course_id conf
                                      (cls1.semester_id = cls1.semester_id and
                                       ct1.weekday = ct2.weekday and
                                       ct1.time_begin <= ct2.time_begin and
                                       ct1.time_end >= ct2.time_end) and       -- course_time conf
                                       cs.sid = ?
                            """;
                    s = conn.prepareStatement(queryConflictCourses);
                    s.setInt(1, entry.section.id);
                    s.setInt(2, studentId);
                    ResultSet conflictRes = s.executeQuery();

                    entry.conflictCourseNames = new ArrayList<>();
                    while (conflictRes.next())
                        entry.conflictCourseNames.add("%s[%s]".formatted(
                                conflictRes.getString("course_name"),
                                conflictRes.getString("class_name")));

                    cnt = cnt + 1;
                    entries.add(entry);
                }
            }
            conn.close();
            return entries;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            ResultSet res;
            PreparedStatement statement;
            EnrollResult enrollResult;

            // COURSE_NOT_FOUND
            String sql = """
                    select count(*) from classes
                    where class_id = ?
                    """;
            statement = conn.prepareStatement(sql);
            statement.setInt(1, sectionId);
            res = statement.executeQuery();
            res.next();
            if (res.getInt(1) == 0) {
                enrollResult = EnrollResult.COURSE_NOT_FOUND;
                return enrollResult;
            }

            // COURSE_IS_FULL
            String sql1 = """
                    with selected as (
                        select count(*) cnt
                        from course_select
                        group by class_id
                        having class_id = ?
                        )
                    select ((select selected.cnt from selected)>= classes.capacity)
                    from classes
                    where class_id = ?
                """;
            statement = conn.prepareStatement(sql1);
            statement.setInt(1, sectionId);
            statement.setInt(2, sectionId);
            res = statement.executeQuery();
            res.next();
            if (res.getBoolean(1)) {
                enrollResult = EnrollResult.COURSE_IS_FULL;
                return enrollResult;
            }

            // ALREADY_ENROLLED
            String sql2 = """
                    select count(*) from course_select
                    where class_id = ? and sid = ?
                """;
            statement = conn.prepareStatement(sql2);
            statement.setInt(1, sectionId);
            statement.setInt(2, studentId);
            res = statement.executeQuery();
            res.next();
            if (res.getInt(1) > 0) {
                enrollResult = EnrollResult.ALREADY_ENROLLED;
                return enrollResult;
            }

            // ALREADY_PASSED
            String sql3 = """
                    select grade from course_select
                    where class_id = ? and sid = ?
                """;
            statement = conn.prepareStatement(sql3);
            statement.setInt(1, sectionId);
            statement.setInt(2, studentId);
            res = statement.executeQuery();
            if (res.next()) {
                if (res.getInt(1) >= 60) {
                    enrollResult = EnrollResult.ALREADY_PASSED;
                    return enrollResult;
                }
            }

            // PRE...NOT
            String sql4 = """
                        select cr.course_id as cid
                        from classes c
                        join courses cr on c.course_id = cr.course_id
                        where class_id = ?
                    """;
            statement = conn.prepareStatement(sql4);
            statement.setInt(1, sectionId);
            res = statement.executeQuery();
            res.next();
            String course_id = res.getString(1);
            if (!passedPrerequisitesForCourse(studentId, course_id)) {
                enrollResult = EnrollResult.PREREQUISITES_NOT_FULFILLED;
                return enrollResult;
            }

            // COURSE_CONFLICT_FOUND,
            String sql5 = """
                    with to_select as (
                        select class_id, time_begin, time_end, weekday
                        from class_timetable
                        where class_id = ?
                    )
                    select count(*)
                    from course_select cs
                    join class_timetable ct on cs.class_id = ct.class_id
                    join to_select on (
                        to_select.weekday = ct.weekday and
                        to_select.time_begin >= ct.time_begin and
                        to_select.time_end <= ct.time_end
                        )
                    where cs.sid = ?
                    """;
            statement = conn.prepareStatement(sql5);
            statement.setInt(1, sectionId);
            statement.setInt(2, studentId);
            res = statement.executeQuery();
            res.next();
            if (res.getInt(1) > 0) {
                enrollResult = EnrollResult.COURSE_CONFLICT_FOUND;
                return enrollResult;
            }

            enrollResult = EnrollResult.SUCCESS;
            conn.close();
            return enrollResult;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = """
                    delete from course_select
                    where sid = ? and
                          class_id = ?
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, studentId);
            statement.setInt(2, sectionId);
            statement.execute();
            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = """
                        insert into course_select (sid, class_id, grade)
                        values (?, ?, ?)
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, studentId);
            statement.setInt(2, studentId);
            short score = 80;
            if (grade.getClass() == PassOrFailGrade.class) {
                if (grade == PassOrFailGrade.PASS)
                    score = 85;
                else score = 59;
            }
            else if (grade.getClass() == HundredMarkGrade.class)
                score = ((HundredMarkGrade) grade).mark;
            statement.setInt(3, score);
            statement.execute();
            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            CourseTable courseTable = new CourseTable();

            // Verify week in certain semester
            String sql = """
                    with semester as (
                        select semester_id, semester_begin, semester_end
                        from semesters
                        where semester_begin <= ? and semester_end >= ?
                    )
                    , query_week as (
                        select (
                            select week
                            from class_week_list
                            where ((select semester_begin from semester) + interval (week - 1) || ' week') <= ? and 
                                  ((select semester_end from semester)   + interval (week - 1) || ' week') >= ?
                        )
                    )
                    select course_name, class_name, 
                           teacher_id, (first_name || ' ' || last_name) as full_name,
                           weekday, time_begin, time_end,
                           location
                    from courses c
                    join classes c2 on c.course_id = c2.course_id
                    join class_timetable ct on c2.class_id = ct.class_id
                    join class_week_list cwl on ct.class_timetable_id = cwl.class_timetable_id
                    join locations l on ct.location_id = l.location_id
                    join class_teachers t on ct.class_timetable_id = t.class_timetable_id
                    join teachers t2 on t.teacher_id = t2.user_id
                    join course_select cs on c2.class_id = cs.class_id
                    where c2.semester_id = (select semester_id from semester) and
                          cwl.week = (select week from query_week) and
                          cs.sid = ?
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setDate(1, date);
            statement.setDate(2, date);
            statement.setDate(3, date);
            statement.setDate(4, date);
            statement.setInt(5, studentId);
            ResultSet res = statement.executeQuery();

            // init table
            for (DayOfWeek dayOfWeek : DayOfWeek.values())
                courseTable.table.put(dayOfWeek, new HashSet<>());

            while (res.next()) {
                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                entry.courseFullName = String.format("%s[%s]", res.getString("course_name"),
                                                               res.getString("class_name"));
                Instructor instructor = new Instructor();
                instructor.id = res.getInt("teacher_id");
                if (FullNameCheck.isChinese(res.getString("full_name").charAt(0)))
                    instructor.fullName = res.getString("full_name").replace(" ", "");
                else
                    instructor.fullName = res.getString("full_name");
                entry.instructor = instructor;
                entry.classBegin = res.getShort("time_begin");
                entry.classEnd = res.getShort("time_end");
                entry.location = res.getString("location");
                courseTable.table.get(DayOfWeek.of(res.getInt("weekday"))).add(entry);
            }
            conn.close();
            return courseTable;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean calculatePrerequisites(Prerequisite node, ArrayList<String> passedCourses) {
        if (node == null)
            return true;
        if (node.getClass().equals(CoursePrerequisite.class)) {
            if ((passedCourses.contains(((CoursePrerequisite) node).courseID)))
                return true;
            else
                return false;
        }
        if (node.getClass().equals(OrPrerequisite.class)) {
            boolean result = false;
            for (Prerequisite son : ((OrPrerequisite) node).terms)
                result = result | calculatePrerequisites(son, passedCourses);
            return result;
        }
        if (node.getClass().equals(AndPrerequisite.class)) {
            boolean result = true;
            for (Prerequisite son : ((AndPrerequisite) node).terms)
                result = result & calculatePrerequisites(son, passedCourses);
            return result;
        }
        return false;
    }

    public boolean passedPrerequisitesForCourse(int studentId, String course_id) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            ArrayList<String> passedCourses = new ArrayList<>();
            
            // Get student passed courses
            String queryPassedCourses = """
                       select distinct c.course_id
                       from course_select cs
                       join classes c on cs.class_id = c.class_id
                       where cs.grade >= 60 and cs.grade is not null and sid = ?
                    """;
            PreparedStatement statement = conn.prepareStatement(queryPassedCourses);
            statement.setInt(1, studentId);
            ResultSet res = statement.executeQuery();
            while (res.next())
                passedCourses.add(res.getString(1));

            // Get prerequisites instance
            String queryPrerequisite = """
                        select prerequisite from courses where course_id = ?
                    """;
            Prerequisite prerequisite = null;
            statement = conn.prepareStatement(queryPrerequisite);
            statement.setString(1, course_id);
            res = statement.executeQuery();
            if (res.next()) {
                ObjectMapper mapper = new ObjectMapper();
                prerequisite = mapper.readValue(res.getString(1), Prerequisite.class);
            }
            conn.close();
            if (prerequisite == null)
                return true;

            // Calculate satisfaction of prerequisites
            return calculatePrerequisites(prerequisite, passedCourses);
        }
        catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
