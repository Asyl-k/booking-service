package dev.asyl.portfolio.booking;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="bookings")
public class Booking {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,length=100)
    private String resourceId;
    @Column(nullable=false,length=100)
    private String customerId;
    @Column(nullable=false)
    private Instant startsAt;
    @Column(nullable=false)
    private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16)
    private BookingStatus status=BookingStatus.CONFIRMED;
    protected Booking(){}
    public Booking(String resourceId,String customerId,Instant startsAt,Instant endsAt){
        if(!endsAt.isAfter(startsAt)) throw new IllegalArgumentException("End must be after start");
        this.resourceId=resourceId;this.customerId=customerId;this.startsAt=startsAt;this.endsAt=endsAt;
    }
    public Long getId(){return id;}
    public String getResourceId(){return resourceId;}
    public String getCustomerId(){return customerId;}
    public Instant getStartsAt(){return startsAt;}
    public Instant getEndsAt(){return endsAt;}
    public BookingStatus getStatus(){return status;}
    public void cancel(){if(status==BookingStatus.CANCELLED)throw new IllegalStateException("Booking already cancelled");status=BookingStatus.CANCELLED;}
}
