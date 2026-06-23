package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "bus_route_stops")
public class BusRouteStopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private BusRouteEntity route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private BusStopEntity stop;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    public BusRouteStopEntity() {
    }

    public long getId() {
        return id;
    }

    public BusRouteEntity getRoute() {
        return route;
    }

    public void setRoute(BusRouteEntity route) {
        this.route = route;
    }

    public BusStopEntity getStop() {
        return stop;
    }

    public void setStop(BusStopEntity stop) {
        this.stop = stop;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

}