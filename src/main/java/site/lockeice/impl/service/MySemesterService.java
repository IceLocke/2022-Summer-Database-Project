package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySemesterService implements SemesterService {
    @Override
    @ParametersAreNonnullByDefault
    public int addSemester(String name, Date begin, Date end) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            
            String addSem = "insert into semesters (semester_name, semester_begin, semester_end)" +
                            "values(?, ?, ?)";
            PreparedStatement s = conn.prepareStatement(addSem);
            s.setString(1, name);
            s.setDate(2, begin);
            s.setDate(3, end);
            s.execute();

            String querySemId = "select max(semester_id) as max_semester_id from semesters";
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(querySemId);
            res.next();
            return res.getInt(1);
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void removeSemester(int semesterId) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            

            // remove week list
            String removeWeekList = """
                        delete from class_week_list
                        where class_timetable_id in (
                            select class_timetable_id
                            from class_timetable ctt
                            join classes c on ctt.class_id = c.class_id
                            where c.semester_id = ?
                        )
                    """;
            PreparedStatement s = conn.prepareStatement(removeWeekList);
            s.setInt(1, semesterId);
            s.execute();

            // remove class timetable
            String removeClassTimetable = """
                        delete from class_timetable
                        where class_id in (
                            select class_id
                            from classes
                            where semester_id = ?
                        )
                    """;
            s = conn.prepareStatement(removeClassTimetable);
            s.setInt(1, semesterId);
            s.execute();

            // remove classes
            String removeClass = """
                        delete from classes
                        where semester_id = ?
                    """;
            s = conn.prepareStatement(removeClass);
            s.setInt(1, semesterId);
            s.execute();

            // remove semester
            String removeSemester = """
                        delete from semesters
                        where semester_id = ?
                    """;
            s = conn.prepareStatement(removeSemester);
            s.setInt(1, semesterId);
            s.execute();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public List<Semester> getAllSemesters() {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            ArrayList<Semester> semesters = new ArrayList<>();
            
            String sql = "select semester_id, semester_name, semester_begin, semester_end from semesters";
            Statement s = conn.createStatement();
            ResultSet res = s.executeQuery(sql);

            while (res.next()) {
                Semester sem = new Semester();
                sem.id = res.getInt(1);
                sem.name = res.getString(2);
                sem.begin = res.getDate(3);
                sem.end = res.getDate(4);
                semesters.add(sem);
            }
            conn.close();
            return semesters;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
