package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "bus_trips")
public class BusTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // trip_id z GTFS
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private BusLine line;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_calendar_id", nullable = false)
    private BusServiceCalendar serviceCalendar;

    @Column(name = "headsign")
    private String headsign;

    @Column(name = "direction_id")
    private Integer directionId;

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public BusLine getLine() {
        return line;
    }

    public BusServiceCalendar getServiceCalendar() {
        return serviceCalendar;
    }

    public String getHeadsign() {
        return headsign;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setLine(BusLine line) {
        this.line = line;
    }

    public void setServiceCalendar(BusServiceCalendar serviceCalendar) {
        this.serviceCalendar = serviceCalendar;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }
}