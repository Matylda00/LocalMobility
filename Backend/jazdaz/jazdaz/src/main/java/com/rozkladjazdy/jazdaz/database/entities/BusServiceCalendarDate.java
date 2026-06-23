package com.rozkladjazdy.jazdaz.database.entities;

import com.rozkladjazdy.jazdaz.database.entities.BusServiceCalendar;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "bus_service_calendar_dates")
public class BusServiceCalendarDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_calendar_id", nullable = false)
    private BusServiceCalendar serviceCalendar;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "exception_type", nullable = false)
    private Integer exceptionType;

    public Long getId() {
        return id;
    }

    public BusServiceCalendar getServiceCalendar() {
        return serviceCalendar;
    }

    public LocalDate getDate() {
        return date;
    }

    public Integer getExceptionType() {
        return exceptionType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setServiceCalendar(BusServiceCalendar serviceCalendar) {
        this.serviceCalendar = serviceCalendar;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setExceptionType(Integer exceptionType) {
        this.exceptionType = exceptionType;
    }
}