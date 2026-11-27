package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "acceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acceso")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    @Column(name = "fecha_hora_entrada", nullable = false)
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_resultado", nullable = false, length = 20)
    private ResultadoAcceso tipoResultado;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_denegacion", length = 255)
    private MotivoDenegacion motivoDenegacion;

    @Column(name = "ticket_parking", length = 100)
    private String ticketParking;


}
