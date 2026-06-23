package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "bus_lines")
public class BusLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "line_number", nullable = false)
    private String lineNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "route_type")
    private Integer routeType;

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public String getName() {
        return name;
    }

    public Integer getRouteType() {
        return routeType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }
}