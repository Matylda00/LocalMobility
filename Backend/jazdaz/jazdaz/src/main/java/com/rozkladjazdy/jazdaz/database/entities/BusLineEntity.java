package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "bus_lines")
public class BusLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column(name = "line_number", nullable = false, unique = true, length = 20)
    private String lineNumber;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "start_stop", length = 100)
    private String startStop;

    @Column(name = "end_stop", length = 100)
    private String endStop;

    @Column(name = "route_type")
    private Integer routeType;

    public BusLineEntity() {
    }

    public long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartStop() {
        return startStop;
    }

    public void setStartStop(String startStop) {
        this.startStop = startStop;
    }

    public String getEndStop() {
        return endStop;
    }

    public void setEndStop(String endStop) {
        this.endStop = endStop;
    }

    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }
}