package es.safareyes.gimnasio.modelos;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "socio")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Socio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "id_socio")
    private Integer id;

    @Column(name = "dni")
    private String dni;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellidos")
    private String apellidos;

    @Column(name = "email")
    private String email;

    @Column(name = "telefono")
    private Integer telefono;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "matricula")
    private Integer matricula;




}
