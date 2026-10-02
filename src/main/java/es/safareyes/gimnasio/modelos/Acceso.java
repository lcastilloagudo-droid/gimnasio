package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acceso")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Acceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_acceso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    @Column(name = "fecha_hora_entrada")
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "feha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    @Column(name = "tipo_resultado")
    private Boolean tipoResultado;

    @Column(name = "motivo_denegacion")
    private String motivoDenegacion;

    @Column(name = "ticket_parking")
    private Boolean nomUsuario;
}
