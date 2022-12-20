package org.codedefenders.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;

public class ScoreDAO {
    public static boolean insert(Integer eventId, String update) {
        String query = String.join("\n", "INSERT INTO scores (Event_ID, Score) VALUES (?, ?);");
        DatabaseValue<?>[] valueList = new DatabaseValue[]{
                DatabaseValue.of(eventId),
                DatabaseValue.of(update)};
        final Connection conn1 = DB.getConnection();
        final PreparedStatement stmt1 = DB.createPreparedStatement(conn1, query, valueList);
        return DB.executeUpdateGetKeys(stmt1, conn1) >= 0;
    }

    public static String findByEventId(Integer eventId) {
        String query = String.join("\n", "SELECT * from scores WHERE Event_ID=?");
        DatabaseValue<?>[] values = new DatabaseValue[]{DatabaseValue.of(eventId)};
        return DB.executeQueryReturnValue(query, rs -> rs.getString("Score"), values);
    }
}
