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
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {

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
        } catch (SQLException e) {
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
        ArrayList<CourseSearchEntry> entries = new ArrayList<>();
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = """
                    select class_id, class_name, capacity,
                                    course_id, course_name, credit, hour, grading, 
                                     prerequisite
                    from (
                        select distinct cls.class_id, cls.class_name, cls.capacity, 
                               crs.course_id, crs.course_name, crs.credit, crs.hour, crs.grading,
                               crs.prerequisite
                        from classes cls
                        join courses crs on crs.course_id = cls.course_id and cls.semester_id = ?
                        join class_timetable ctt on cls.class_id = ctt.class_id
                        join locations l on ctt.location_id = l.location_id
                        join class_teachers ct on ctt.class_timetable_id = ct.class_timetable_id
                        join teachers t on t.user_id = ct.teacher_id
                        where 
                              (? or cls.course_id like ?) and
                              (? or crs.course_name || '[' || cls.class_name || ']' like ?) and
                              (
                                ? or
                                (
                                    (t.first_name || ' ' || t.last_name like ?) or
                                    (t.first_name || t.last_name like ?)
                                ) 
                               ) and
                              (ctt.weekday = ? or ?) and
                              ((ctt.time_begin <= ? and ctt.time_end >= ?) or ?) and
                              (l.location like any(?) or ?)
                    ) as subq
                    where 
                          (? 
                          or subq.class_id not in (
                                select cls2.class_id
                                from course_select cs
                                join classes cls1 on (cs.sid = ? and cs.grade >= 60) and 
                                                         cs.class_id = cls1.class_id
                                join classes cls2 on cls1.course_id = cls2.course_id
                          )
                          ) -- passed check
                    order by course_id, course_name, class_name
                    -- limit ? offset ?
                    """;

            /**
             * REMEMBER TO DELETE!!!
             */
            // ignorePassed = true;

            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, semesterId);
            statement.setString(3, searchCid == null ? "" : "%" + searchCid + "%");
            statement.setBoolean(2, searchCid == null);
            statement.setString(5, searchName == null ? "" : "%" + searchName + "%");
            statement.setBoolean(4, searchName == null);
            statement.setString(7, searchInstructor == null ? "" : "%" + searchInstructor + "%");
            statement.setString(8, searchInstructor == null ? "" : "%" + searchInstructor + "%");
            statement.setBoolean(6, searchInstructor == null);
            statement.setInt(9, searchDayOfWeek == null ? 0 : searchDayOfWeek.getValue());
            statement.setBoolean(10, searchDayOfWeek == null);
            statement.setInt(11, searchClassTime == null ? 0 : searchClassTime);
            statement.setInt(12, searchClassTime == null ? 0 : searchClassTime);
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
            statement.setBoolean(16, ignorePassed);
            statement.setInt(17, studentId);

            //System.out.println("Query all");
            ResultSet res = statement.executeQuery();

            /***
             * REMEMBER TO DELETE!!!
             */
            ignoreMissingPrerequisites = !ignoreMissingPrerequisites;

            int cnt = 1;

            while (res.next() && entries.size() < pageSize) {
                if (ignoreMissingPrerequisites ||
                        passedPrerequisitesForCourse(studentId, res.getString("course_id"), conn)) {
                    if (cnt > pageSize + pageSize * pageIndex ||
                            cnt <= pageSize * pageIndex) {
                        cnt++;
                        continue;
                    }

                    // Course type part
                    String courseID = res.getString("course_id");
                    if (searchCourseType != CourseType.ALL) {
                        String queryStudentMajor = """
                                    select major_id from students where sid = ?
                                """;
                        PreparedStatement statement1 = conn.prepareStatement(queryStudentMajor);
                        statement1.setInt(1, studentId);
                        ResultSet resultSet = statement1.executeQuery();

                        if (resultSet.next()) {
                            int majorId = resultSet.getInt(1);
                            String queryCourseType = """
                                        select major_id, type
                                        from course_type
                                        where course_id = ? and type is not null and type != 0
                                    """;
                            statement1 = conn.prepareStatement(queryCourseType);
                            statement1.setString(1, courseID);
                            resultSet = statement1.executeQuery();
                            boolean isMajorCompulsory = false,
                                    isMajorElective = false,
                                    inInOtherMajor = false;

                            while (resultSet.next()) {
                                if (resultSet.getInt("major_id") == majorId &&
                                        resultSet.getInt("type") == CourseType.MAJOR_COMPULSORY.ordinal())
                                    isMajorCompulsory = true;
                                if (resultSet.getInt("major_id") == majorId &&
                                        resultSet.getInt("type") == CourseType.MAJOR_ELECTIVE.ordinal())
                                    isMajorElective = true;
                                inInOtherMajor = true;
                            }

                            if (searchCourseType == CourseType.MAJOR_COMPULSORY)
                                if (!isMajorCompulsory)
                                    continue;
                            if (searchCourseType == CourseType.MAJOR_ELECTIVE)
                                if (!isMajorElective)
                                    continue;
                            if (searchCourseType == CourseType.CROSS_MAJOR)
                                if (!inInOtherMajor && isMajorCompulsory && isMajorElective)
                                    continue;
                            if (searchCourseType == CourseType.PUBLIC)
                                if (inInOtherMajor)
                                    continue;
                        }
                    }

                    // Course info part
                    //System.out.println("%s Course info".formatted(res.getString("course_id")));
                    CourseSearchEntry entry = new CourseSearchEntry();
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
                    //System.out.println("%s Course section".formatted(res.getString("course_id")));

                    entry.section = new CourseSection();
                    entry.section.id = res.getInt("class_id");
                    entry.section.name = res.getString("class_name");
                    entry.section.totalCapacity = res.getInt("capacity");

                    // Get capacity
                    String queryCapacity = """
                                select count(*) from course_select
                                where class_id = ?
                            """;
                    PreparedStatement queryCapacityStatement = conn.prepareStatement(queryCapacity);
                    queryCapacityStatement.setInt(1, entry.section.id);
                    ResultSet selectCount = queryCapacityStatement.executeQuery();
                    selectCount.next();
                    entry.section.leftCapacity = entry.section.totalCapacity - selectCount.getInt(1);
                    selectCount.close();
                    queryCapacityStatement.close();

                    // Get section classes by class_id
                    //System.out.println("%s Course section class".formatted(res.getString("course_id")));

                    entry.sectionClasses = new HashSet<>();
                    String queryCourseSectionClass = """
                                select distinct ctt.class_timetable_id,
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
                        p.close();
                        weekListRes.close();
                    }
                    sectionClassRes.close();

                    // Get conflict courses names
                    // System.out.println("%s Course section conflict".formatted(res.getString("course_id")));
                    String queryConflictCourses = """
                                select course_name, class_name
                                from 
                                    (
                                    with to_select as (
                                        select c.class_id, c.course_id, time_begin, time_end, weekday, c.semester_id
                                        from class_timetable
                                        join classes c on class_timetable.class_id = c.class_id
                                        where c.class_id = ?
                                    )
                                    select distinct cs.class_id
                                    from course_select cs
                                    join class_timetable ct on cs.class_id = ct.class_id
                                    join classes c on cs.class_id = c.class_id
                                    join to_select on 
                                        (
                                            to_select.weekday = ct.weekday and
                                            (
                                                (
                                                    to_select.time_begin <= ct.time_end and 
                                                    to_select.time_end >= ct.time_begin 
                                                ) or
                                                (
                                                    ct.time_begin <= to_select.time_end and
                                                    ct.time_end >= to_select.time_begin
                                                )
                                            ) and
                                            to_select.semester_id = c.semester_id
                                        ) or
                                        (
                                            to_select.course_id = c.course_id and
                                            to_select.semester_id = c.semester_id
                                        )
                                    where cs.sid = ?
                                    ) subq
                                join classes on subq.class_id = classes.class_id
                                join courses on classes.course_id = courses.course_id
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
                    entry.conflictCourseNames.sort(Comparator.naturalOrder());
                    conflictRes.close();
                    cnt = cnt + 1;
                    entries.add(entry);
                }
            }
//            System.out.println("finished %d".formatted(studentId));
            conn.close();
            return entries;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
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
                        select (select course_id from classes where class_id = ?) in (
                        select c.course_id
                        from course_select
                        join classes c on course_select.class_id = c.class_id
                        where sid = ? and grade >= 60);
                    """;
            statement = conn.prepareStatement(sql3);
            statement.setInt(1, sectionId);
            statement.setInt(2, studentId);
            res = statement.executeQuery();
            while (res.next()) {
                if (res.getBoolean(1)) {
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
            if (!passedPrerequisitesForCourse(studentId, course_id, conn)) {
                enrollResult = EnrollResult.PREREQUISITES_NOT_FULFILLED;
                return enrollResult;
            }

            // COURSE_CONFLICT_FOUND,
            String sql5 = """
                    with to_select as (
                        select c.class_id, c.course_id, time_begin, time_end, weekday, c.semester_id
                        from class_timetable
                        join classes c on class_timetable.class_id = c.class_id
                        where c.class_id = ?
                    )
                    select count(*)
                    from course_select cs
                    join class_timetable ct on cs.class_id = ct.class_id
                    join classes c on c.class_id = ct.class_id
                    join to_select on 
                        (
                            to_select.weekday = ct.weekday and
                            (
                                (
                                    to_select.time_begin <= ct.time_end and 
                                    to_select.time_end >= ct.time_begin 
                                ) or
                                (
                                    ct.time_begin <= to_select.time_end and
                                    ct.time_end >= to_select.time_begin
                                )
                            ) and
                            to_select.semester_id = c.semester_id
                        ) or
                        (
                            to_select.course_id = c.course_id and
                            to_select.semester_id = c.semester_id
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

            // COURSE_IS_FULL
//            String sql1 = """
//                        with selected as (
//                            select count(*) cnt
//                            from course_select
//                            group by class_id
//                            having class_id = ?
//                            )
//                        select ((select selected.cnt from selected)>= classes.capacity)
//                        from classes
//                        where class_id = ?
//                    """;
//            statement = conn.prepareStatement(sql1);
//            statement.setInt(1, sectionId);
//            statement.setInt(2, sectionId);
//            res = statement.executeQuery();
//            res.next();
//            if (res.getBoolean(1)) {
//                enrollResult = EnrollResult.COURSE_IS_FULL;
//                return enrollResult;
//            }

            //System.out.println("Success");
            enrollResult = EnrollResult.SUCCESS;
            String enrollSql = """
                    insert into course_select (sid, class_id)
                    values (?, ?)
                    """;
            statement = conn.prepareStatement(enrollSql);
            statement.setInt(1, studentId);
            statement.setInt(2, sectionId);
            statement.execute();

            conn.close();
            return enrollResult;
        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String hasEnrolled = """
                    select grade from course_select
                    where sid = ? and class_id = ? and grade is not null
                    """;
            PreparedStatement statement = conn.prepareStatement(hasEnrolled);
            statement.setInt(1, studentId);
            statement.setInt(2, sectionId);
            ResultSet res = statement.executeQuery();
            if (res.next()){
                if (res.getInt(1) >= 0 && res.getInt(1) <= 100)
                    throw new IllegalStateException();
            }

            String sql = """
                    delete from course_select
                    where sid = ? and
                          class_id = ?
                    """;
            statement = conn.prepareStatement(sql);
            statement.setInt(1, studentId);
            statement.setInt(2, sectionId);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {

            String sql = """
                        insert into course_select (sid, class_id, grade)
                        values (?, ?, ?)
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, studentId);
            statement.setInt(2, sectionId);
            short score = 80;
            if (grade != null) {
                if (grade.getClass() == PassOrFailGrade.class) {
                    if (grade == PassOrFailGrade.PASS)
                        score = 85;
                    else score = 59;
                } else if (grade.getClass() == HundredMarkGrade.class) {
                    if (((HundredMarkGrade) grade).mark > 100 || ((HundredMarkGrade) grade).mark < 0)
                        throw new IllegalStateException();
                    score = ((HundredMarkGrade) grade).mark;
                }
                statement.setInt(3, score);
            }
            else statement.setNull(3, Types.INTEGER);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable courseTable = new CourseTable();
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            // Verify week in certain semester
            String sql = """
                    with semester as (
                        select semester_id, semester_begin, semester_end
                        from semesters
                        where semester_begin <= ? and semester_end >= ?
                    )
                    , query_week as (
                        select distinct week
                        from class_week_list
                        where ? between
                            ((select semester_begin from semester limit 1) + cast((week - 1) * 7 as integer)) and
                            ((select semester_begin from semester limit 1) + cast(week * 7 as integer))
                        limit 1
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
            statement.setInt(4, studentId);
            ResultSet res = statement.executeQuery();

            // init table
            courseTable.table = new HashMap<>();
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseTable;
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

    public boolean passedPrerequisitesForCourse(int studentId, String course_id, Connection conn) {
        try {
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
            statement.close();
            if (prerequisite == null)
                return true;

            // Calculate satisfaction of prerequisites
            return calculatePrerequisites(prerequisite, passedCourses);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
