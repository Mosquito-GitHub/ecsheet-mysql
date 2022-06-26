package com.mars.ecsheet.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Table(name = "worksheet")
@Entity
@Getter
@Setter
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class WorkSheetEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    @Column(name = "id",unique = true)
    private String id;

    @Column(name = "workbook_id")
    private String wbId;

    @Type(type = "json")
    @Column(name = "data")
    private JSONObject data;

    /**
     * 删除标记,0是未删除，1是删除
     */
    private int deleteStatus;

    public WorkSheetEntity() {
    }

    public WorkSheetEntity(String id, String wbId, JSONObject data, int deleteStatus) {
        this.id = id;
        this.wbId = wbId;
        this.data = data;
        this.deleteStatus = deleteStatus;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}
