package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuota")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuota")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    // Plan que tenia el socio cuando se genero esta cuota.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @Column(name = "mes", nullable = false)
    private Short mes;

    @Column(name = "anio", nullable = false)
    private Short anyo;

    @Column(name = "importe", nullable = false)
    private BigDecimal importe;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;
}
