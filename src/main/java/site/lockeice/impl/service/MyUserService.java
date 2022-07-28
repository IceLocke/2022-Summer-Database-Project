package site.lockeice.impl.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService {
    @Override
    public void removeUser(int userId) {

    }

    @Override
    public List<User> getAllUsers() {
        try {
            ArrayList users = new ArrayList<User>();
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            String sql = "select user_id, (first_name || ' ' || last_name) from teachers";
            Statement statement = conn.createStatement();
            ResultSet res = statement.executeQuery(sql);
            while (res.next()) {
                User user = new Instructor();
                user.id = res.getInt(1);
                user.fullName = res.getString(2);
                users.add(user);
            }
            return users;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
