package ch.plaintext.demo.tabelle;

import ch.plaintext.framework.SuperModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the demo table: enough columns of every kind to show what pt:tableSettings does. */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "demo_product")
public class DemoProduct extends SuperModel {

    private String sku;
    private String name;
    private String category;
    private String supplier;
    private String warehouse;
    private Integer quantity;
    private String unit;
    private BigDecimal price;
    private String currency;
    private BigDecimal vatRate;
    private BigDecimal weightKg;
    private String color;
    private String status;
    private Boolean active;
    private LocalDate releaseDate;
    private LocalDate lastDelivery;
    private String owner;
    private Integer rating;
    private String country;
    private String ean;
    private String notes;
}
