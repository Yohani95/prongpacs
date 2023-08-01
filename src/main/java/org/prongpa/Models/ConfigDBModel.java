package org.prongpa.Models;

import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;

import javax.persistence.*;

@Data
@Entity
@Table(name= "configshell")
public class ConfigDBModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id_configshell;
    @Column(name = "`max_threads`")
    @Type(type = "text")
    private String maxThreads;
    @Column(name = "`max_reintentos`")
    @Type(type = "text")
    private String maxReintentos;
    @Column(name = "`acs_restart_time`")
    @Type(type = "text")
    private String acsRestartTime;
}
