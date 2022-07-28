package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.Instructor;
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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
                        select count(*) cnt from course_select
                        group by class_id
                        having class_id = ?
                        )
                    select (selected.cnt >= capacity)
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
            conn.close();
            if (res.getInt(1) > 0) {
                enrollResult = EnrollResult.COURSE_CONFLICT_FOUND;
                return enrollResult;
            }

            enrollResult = EnrollResult.SUCCESS;
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
                courseTable.table.put(dayOfWeek, new HashSet<CourseTable.CourseTableEntry>());

            while (res.next()) {
                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                entry.courseFullName = String.format("%s[%s]", res.getString("course_name"),
                                                               res.getString("class_name"));
                Instructor instructor = new Instructor();
                instructor.id = res.getInt("teacher_id");
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
                       where cs.grade >= 60 and cs.grade is not null
                    """;
            PreparedStatement statement = conn.prepareStatement(queryPassedCourses);
            ResultSet res = statement.executeQuery();
            while (res.next())
                passedCourses.add(res.getString(1));

            // Get prerequisites instance
            String queryPrerequisite = """
                        select prerequisite from courses where course_id = ? and prerequisite is not null
                    """;
            Prerequisite prerequisite = null;
            statement = conn.prepareStatement(queryPrerequisite);
            statement.setString(1, course_id);
            res = statement.executeQuery();
            if (res.next()) {
                ObjectMapper mapper = new ObjectMapper();
                prerequisite = mapper.readValue(res.getString(1), Prerequisite.class);
            }
            else
                return true;

            // Calculate satisfaction of prerequisites
            conn.close();
            return calculatePrerequisites(prerequisite, passedCourses);
        }
        catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
