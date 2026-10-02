package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table (name = "usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_usuario")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_socio", nullable = false)
    private Socio idSocio;

    @Column(name = "nom_usuario")
    private String nomUsuario;

    @Column(name = "contrasenya")
    private String contrasenya;

    @Column(name = "rol")
    @Enumerated(EnumType.ORDINAL)
    private Rol rol;


}
