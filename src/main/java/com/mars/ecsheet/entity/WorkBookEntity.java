package com.mars.ecsheet.entity;

import com.alibaba.fastjson.JSONObject;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "workbook")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class WorkBookEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    @Column(name = "id",unique = true)
    private String id;

    @Column(name = "name")
    private String name;

    @Type(type = "json")
    @Column(name = "options")
    private JSONObject option;

//    private Date createdTime;
//
//    private Date modifyTime;
//
//    private List<String> userId;
}
