package org.prongpa.Models;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "tasks_historico")
public class TaskHistoryModel {
    @Id
    private int id;
    private String estado;
    @Column(name = "`last_date`")
    private Date lastDate;
    // @Column(name = "`process_id`")
    private String process_id;
    private Integer thread_id;
    private String model;
    private int reintentos;
    private String version;
    private String tasktype;
    private String idcompuesto;
    private String filename;
    private String manufacturer;
    @Column(name = "`notification_id`")
    private String notificationId;
    private String commands;
    @Column(name = "`order`")
    private int orderTask;
    private String mercado;
}
