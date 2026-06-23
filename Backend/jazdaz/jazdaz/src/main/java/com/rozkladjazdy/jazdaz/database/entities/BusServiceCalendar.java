package com.rozkladjazdy.jazdaz.database.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "bus_service_calendars")
public class BusServiceCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false, unique = true)
    private String serviceId;

    @Column(name = "monday", nullable = false)
    private boolean monday;

    @Column(name = "tuesday", nullable = false)
    private boolean tuesday;

    @Column(name = "wednesday", nullable = false)
    private boolean wednesday;

    @Column(name = "thursday", nullable = false)
    private boolean thursday;

    @Column(name = "friday", nullable = false)
    private boolean friday;

    @Column(name = "saturday", nullable = false)
    private boolean saturday;

    @Column(name = "sunday", nullable = false)
    private boolean sunday;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public Long getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isMonday() {
        return monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public boolean isSunday() {
        return sunday;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}