package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.service.StudentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyCourseService implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit,
                          int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = """
                        insert into courses
                        (course_id, course_name, credit, hour, grading, prerequisite) 
                        values (?, ?, ?, ?, ?, ?)
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, courseId);
            statement.setString(2, courseName);
            statement.setInt(3, credit);
            statement.setInt(4, classHour);
            switch (grading) {
                case HUNDRED_MARK_SCORE -> statement.setString(5, "HM");
                case PASS_OR_FAIL -> statement.setString(5, "PF");
            }
            ObjectMapper mapper = new ObjectMapper();
            statement.setString(6, mapper.writeValueAsString(prerequisite));

            statement.execute();

            String addCourseType = """
                        insert into course_type
                        (course_id, course_type)
                        values (?, ?)
                    """;
            statement = conn.prepareStatement(addCourseType);
            statement.setString(1, courseId);
            statement.setInt(2, StudentService.CourseType.ALL.ordinal());
            statement.execute();
            conn.close();
        }
        catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = """
                        insert into classes
                        (course_id, semester_id, class_name, capacity)
                        values (?, ?, ?, ?)
                    """;
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, courseId);
            statement.setInt(2, semesterId);
            statement.setString(3, sectionName);
            statement.setInt(4, totalCapacity);
            statement.execute();

            String querySection = """
                        select max(class_id) 
                        from classes cls
                        join courses c on c.course_id = cls.course_id
                        where class_name = ? and c.course_id = ?
                    """;
            PreparedStatement s = conn.prepareStatement(querySection);
            s.setString(1, sectionName);
            s.setString(2, courseId);
            ResultSet res = s.executeQuery();
            res.next();

            // sectionID
            int result = res.getInt(1);
            conn.close();

            return result;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId,
                                     DayOfWeek dayOfWeek, Set<Short> weekList,
                                     short classStart, short classEnd,
                                     String location
    ) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            // Specify location
            String queryLocation = "select location_id from locations where location = ?";
            PreparedStatement s1 = conn.prepareStatement(queryLocation);
            s1.setString(1, location);
            ResultSet res = s1.executeQuery();
            int locationId;
            if (res.next()) {
            }
            else {
                String insertLocation = "insert into locations (location)" +
                                        "values (?)";
                PreparedStatement s3 = conn.prepareStatement(insertLocation);
                s3.setString(1, location);
                s3.execute();
                String queryLocationCount =  "select location_id from locations where location = '%s'";
                Statement s2 = conn.createStatement();
                res = s2.executeQuery(queryLocationCount.formatted(location));
                res.next();
            }
            locationId = res.getInt(1);

            // add section class
            String addClass = """
                        insert into class_timetable
                        (class_id, weekday, time_begin, time_end, location_id)
                        values(?, ?, ?, ?, ?)
                    """;
            PreparedStatement s4 = conn.prepareStatement(addClass);
            s4.setInt(1, sectionId);
            s4.setInt(2, dayOfWeek.getValue());
            s4.setInt(3, classStart);
            s4.setInt(4, classEnd);
            s4.setInt(5, locationId);
            s4.execute();

            String queryClassTTId = "select currval(pg_get_serial_sequence('class_timetable', 'class_timetable_id'))";
            Statement s5 = conn.createStatement();
            res = s5.executeQuery(queryClassTTId);
            res.next();
            int classTTId = res.getInt(1);

            String addWeekList = "insert into class_week_list (class_timetable_id, week)" +
                                 "values(?, ?)";
            PreparedStatement s6 = conn.prepareStatement(addWeekList);
            s6.setInt(1, classTTId);
            for (short week : weekList) {
                s6.setInt(2, week);
                s6.execute();
            }

            String addTeacher = "insert into class_teachers (class_timetable_id, teacher_id) " +
                                "values (?, ?)";
            PreparedStatement s7 = conn.prepareStatement(addTeacher);
            s7.setInt(1, classTTId);
            s7.setInt(2, instructorId);
            s7.execute();

            conn.close();
            return classTTId;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void removeCourse(String courseId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            // delete class_week_list
            String deleteWeekList = """
                        delete from class_week_list cwl
                        where cwl.class_timetable_id in (
                            select ctt.class_timetable_id
                            from class_timetable ctt
                            join classes c on ctt.class_id = c.class_id
                            where c.course_id = ?
                        )
                    """;
            PreparedStatement s = conn.prepareStatement(deleteWeekList);
            s.setString(1, courseId);
            s.execute();

            // delete teacher_class
            String deleteClassTeacher = """
                        delete from class_teachers ct
                        where ct.class_timetable_id in (
                            select ctt.class_timetable_id
                            from class_timetable ctt
                            join classes c on ctt.class_id = c.class_id
                            where c.course_id = ?
                        )
                    """;
            s = conn.prepareStatement(deleteClassTeacher);
            s.setString(1, courseId);
            s.execute();

            // delete class_timetable
            String deleteClassTimetable = """
                        delete from class_timetable ctt
                        where ctt.class_timetable_id in (
                            select ctt.class_timetable_id
                            from class_timetable ctt
                            join classes c on ctt.class_id = c.class_id
                            where c.course_id = ?
                        )
                    """;
            s = conn.prepareStatement(deleteClassTimetable);
            s.setString(1, courseId);
            s.execute();

            // delete classes
            String deleteClass = """
                        delete from classes
                        where course_id = ?
                    """;
            s = conn.prepareStatement(deleteClass);
            s.setString(1, courseId);
            s.execute();

            // delete course
            String deleteCourse = """
                        delete from courses
                        where course_id = ?
                    """;
            s = conn.prepareStatement(deleteCourse);
            s.setString(1, courseId);
            s.execute();

            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            String sql = """
                    select course_id, course_name, credit, hour, grading from courses
                    """;
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(sql);
            ArrayList courses = new ArrayList<Course>();
            while (res.next()) {
                Course course = new Course();
                course.id = res.getString(1);
                course.name = res.getString(2);
                course.credit = res.getInt(3);
                course.classHour = res.getInt(4);
                if (res.getString(5).equals("HM"))
                    course.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;
                else if (res.getString(5).equals("PF"))
                    course.grading = Course.CourseGrading.PASS_OR_FAIL;
                courses.add(course);
            }

            conn.close();
            return courses;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
