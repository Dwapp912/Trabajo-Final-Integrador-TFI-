package Dao;

import java.sql.Connection;
import java.util.List;

public interface GenericDAO<T> {
    // Esta es una interfaz genérica que define métodos comunes para trabajar con cualquier entidad.
    // Sirve como base para evitar repetir código en distintas clases DAO (como PersonaDAO o DomicilioDAO).

    void insertar(T entidad) throws Exception;
    void insertTx(T entidad, Connection conn) throws Exception;
    void actualizar(T entidad)throws Exception;
    void eliminar(int id)throws Exception;
    T getById(int id)throws Exception;
    List<T> getAll()throws Exception;

}
