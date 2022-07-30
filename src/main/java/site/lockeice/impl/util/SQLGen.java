package site.lockeice.impl.util;

import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.List;

public class SQLGen {
    public static String courseSearchSQLGen(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName,
                                            @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
                                            @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, StudentService.CourseType searchCourseType,
                                            boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites,
                                            int pageSize, int pageIndex) {
        String sql = "";
        return sql;
    }
}
