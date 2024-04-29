package ricky.easybrowser.entity.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    @Query("SELECT COUNT(*) FROM history")
    long count();

    @Query("SELECT * FROM history")
    List<History> getAll();


    // Lọc theo thứ tự mới nhất đến cũ nhất
    @Query("SELECT * FROM history ORDER BY id DESC LIMIT ((:pageNo - 1) * :pageSize), :pageSize")
    List<History> getHistory(int pageNo, int pageSize);

    @Insert
    Long insertHistory(History history);

    @Delete
    Void deleteHistory(History history);

    @Query("DELETE FROM history")
    void deleteAll();

}
