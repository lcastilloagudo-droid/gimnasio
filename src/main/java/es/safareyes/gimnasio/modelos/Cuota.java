package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "cuota")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_cuota")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    @Column(name = "mes")
    private LocalDateTime mes;

    @Column(name = "anyo")
    private Date anyo;

    @Column(name = "importe")
    private Integer importe;

    @Column(name = "estado")
    private Boolean estado;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;
}
