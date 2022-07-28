package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
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
            statement.executeQuery();
            conn.commit();
            conn.close();
        }
        catch (SQLException e) {
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
            res.next();
            if (res.getInt(1) >= 60) {
                enrollResult = EnrollResult.ALREADY_PASSED;
                return enrollResult;
            }

            // PRE...NOT
            String sql4 = """
                        select cr.dept_id || cr.course_id_suffix as cid
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
        }
        catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
        return null;
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
            statement.executeQuery();
            conn.commit();
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
            statement.executeQuery();
            conn.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
    }

    public boolean passedPrerequisitesForCourse(int studentId, String course_id) {
        return true;
    }
}
