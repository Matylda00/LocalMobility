package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "bus_stop_times")
public class BusStopTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private BusTrip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private BusStop stop;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "arrival_time", nullable = false)
    private String arrivalTime;

    @Column(name = "departure_time", nullable = false)
    private String departureTime;

    @Column(name = "arrival_seconds", nullable = false)
    private Integer arrivalSeconds;

    @Column(name = "departure_seconds", nullable = false)
    private Integer departureSeconds;

    public Long getId() {
        return id;
    }

    public BusTrip getTrip() {
        return trip;
    }

    public BusStop getStop() {
        return stop;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public Integer getArrivalSeconds() {
        return arrivalSeconds;
    }

    public Integer getDepartureSeconds() {
        return departureSeconds;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTrip(BusTrip trip) {
        this.trip = trip;
    }

    public void setStop(BusStop stop) {
        this.stop = stop;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setArrivalSeconds(Integer arrivalSeconds) {
        this.arrivalSeconds = arrivalSeconds;
    }

    public void setDepartureSeconds(Integer departureSeconds) {
        this.departureSeconds = departureSeconds;
    }
}