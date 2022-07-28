package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import java.sql.*;
import java.util.List;

public class MySemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String addSem = "insert into semesters (semester_name, semester_begin, semester_end)" +
                            "values(?, ?, ?)";
            PreparedStatement s = conn.prepareStatement(addSem);
            s.setString(1, name);
            s.setDate(2, begin);
            s.setDate(3, end);
            s.executeQuery();

            String querySemId = "select max(semester_id) from semesters";
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
    public void removeSemester(int semesterId) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
