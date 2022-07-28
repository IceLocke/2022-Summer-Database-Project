package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public class MyCourseService implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit,
                          int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {

    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
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
            statement.executeQuery();
            conn.commit();

            String querySection = """
                        select max(class_id) from classes
                    """;
            Statement s = conn.createStatement();
            ResultSet res = s.executeQuery(querySection);
            res.next();

            // sectionID
            return res.getInt(1);
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
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();

            // Specify location
            String queryLocation = "select location_id from locations where location = ?";
            PreparedStatement s1 = conn.prepareStatement(queryLocation);
            s1.setString(1, location);
            ResultSet res = s1.executeQuery();
            int locationId = 0;
            if (res.next())
                locationId = res.getInt(1);
            else {
                String queryLocationCount =  "select max(class_timetable_id) from locations";
                Statement s2 = conn.createStatement();
                res = s2.executeQuery(queryLocationCount);
                res.next(); locationId = res.getInt(1) + 1;

                String insertLocation = "insert into locations (location_id, location)" +
                                        "values (?, ?)";
                PreparedStatement s3 = conn.prepareStatement(insertLocation);
                s3.setInt(1, locationId);
                s3.setString(2, location);
                s3.executeQuery();
            }

            // add section class
            String addClass = """
                        insert into class_timetable
                        (class_id, teacher_id, weekday, time_begin, time_end)
                        values(?, ?, ?, ?, ?)
                    """;
            PreparedStatement s4 = conn.prepareStatement(addClass);
            s4.setInt(1, sectionId);
            s4.setInt(2, instructorId);
            s4.setInt(3, dayOfWeek.getValue());
            s4.setInt(4, classStart);
            s4.setInt(5, classEnd);
            s4.executeQuery();

            String queryClassTTId = "select max(class_timetable_id) from class_timetable";
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
                s6.executeQuery();
            }

            conn.commit();
            return classTTId;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();

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
            s.executeQuery();

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
            s.executeQuery();

            // delete classes
            String deleteClass = """
                        delete from classes
                        where course_id = ?
                    """;
            s = conn.prepareStatement(deleteClass);
            s.setString(1, courseId);
            s.executeQuery();

            // delete course
            String deleteCourse = """
                        delete from courses
                        where course_id = ?
                    """;
            s = conn.prepareStatement(deleteCourse);
            s.setString(1, courseId);
            s.executeQuery();

            conn.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        return null;
    }
}
