package com.mars.ecsheet.repository;

import com.mars.ecsheet.entity.WorkBookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Repository
public interface WorkBookRepository extends JpaRepository<WorkBookEntity,String> {

    @Query(value = "SELECT id, name, `options` " +
            "FROM workbook order by create_time desc", nativeQuery = true)
    List<WorkBookEntity> findAll();
}
