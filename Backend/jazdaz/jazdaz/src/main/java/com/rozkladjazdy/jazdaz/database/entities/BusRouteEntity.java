package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus_routes")
public class BusRouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 255)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private BusLineEntity line;

    @Column(name = "direction_id")
    private Integer directionId;

    @Column(name = "headsign", length = 255)
    private String headsign;

    @Column(name = "shape_id", length = 255)
    private String shapeId;

    @Column(name = "gtfs_trip_id", length = 255)
    private String gtfsTripId;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopSequence ASC")
    private List<BusRouteStopEntity> stops = new ArrayList<>();

    public BusRouteEntity() {
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

    public BusLineEntity getLine() {
        return line;
    }

    public void setLine(BusLineEntity line) {
        this.line = line;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public String getShapeId() {
        return shapeId;
    }

    public void setShapeId(String shapeId) {
        this.shapeId = shapeId;
    }

    public String getGtfsTripId() {
        return gtfsTripId;
    }

    public void setGtfsTripId(String gtfsTripId) {
        this.gtfsTripId = gtfsTripId;
    }

    public List<BusRouteStopEntity> getStops() {
        return stops;
    }

    public void setStops(List<BusRouteStopEntity> stops) {
        this.stops = stops;
    }

    public void addStop(BusRouteStopEntity routeStop) {
        stops.add(routeStop);
        routeStop.setRoute(this);
    }

    public void clearStops() {
        for (BusRouteStopEntity stop : stops) {
            stop.setRoute(null);
        }
        stops.clear();
    }
}