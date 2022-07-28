package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.service.InstructorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MyInstructorService implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "insert into teachers (user_id, first_name, last_name)" +
                         "values(?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.executeQuery();

            conn.commit();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
