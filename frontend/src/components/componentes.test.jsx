import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConfirmarCumplidaModal from './ConfirmarCumplidaModal';
import FiltrosMetas from './FiltrosMetas';
import { FILTROS_POR_DEFECTO } from '../utils/filtrosMetas';

afterEach(cleanup);

describe('HU6 · ConfirmarCumplidaModal', () => {
  const meta = { codigoMeta: 1, nombre: 'Leer 12 libros' };
  const abrir = (props = {}) => {
    const onConfirmar = vi.fn();
    const onCancelar = vi.fn();
    render(<ConfirmarCumplidaModal meta={meta} guardando={false} error={null}
      onConfirmar={onConfirmar} onCancelar={onCancelar} {...props} />);
    return { onConfirmar, onCancelar };
  };

  it('se anuncia como diálogo, nombra la meta y deja el foco en «Marcar como cumplida»', () => {
    abrir();
    const dialogo = screen.getByRole('alertdialog', { name: '¿Marcar esta meta como cumplida?' });
    expect(dialogo).toHaveTextContent('Leer 12 libros');
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Marcar como cumplida' }));
  });

  it('confirmar llama a onConfirmar una vez', async () => {
    const { onConfirmar, onCancelar } = abrir();
    await userEvent.click(screen.getByRole('button', { name: 'Marcar como cumplida' }));
    expect(onConfirmar).toHaveBeenCalledTimes(1);
    expect(onCancelar).not.toHaveBeenCalled();
  });

  it('Cancelar, la X y Esc cierran sin marcar', async () => {
    const { onConfirmar, onCancelar } = abrir();
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar ventana' }));
    await userEvent.keyboard('{Escape}');
    expect(onCancelar).toHaveBeenCalledTimes(3);
    expect(onConfirmar).not.toHaveBeenCalled();
  });

  it('mientras guarda, los botones se deshabilitan y Esc no cierra (evita el doble clic)', async () => {
    const { onCancelar } = abrir({ guardando: true });
    expect(screen.getByRole('button', { name: 'Marcando...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    await userEvent.keyboard('{Escape}');
    expect(onCancelar).not.toHaveBeenCalled();
  });

  it('muestra el error del servidor como alerta', () => {
    abrir({ error: 'No se pudo conectar con el servidor' });
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo conectar con el servidor');
  });
});

describe('HU27 · FiltrosMetas', () => {
  const pintar = (filtros = FILTROS_POR_DEFECTO, extra = {}) => {
    const onCambiar = vi.fn();
    const onLimpiar = vi.fn();
    render(<FiltrosMetas filtros={filtros} cantidad={3} cargando={false}
      onCambiar={onCambiar} onLimpiar={onLimpiar} {...extra} />);
    return { onCambiar, onLimpiar };
  };

  it('por defecto «Activas» está presionado y no aparece «Limpiar filtros»', () => {
    pintar();
    expect(screen.getByRole('button', { name: 'Activas' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Todas' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).toBeNull();
    expect(screen.getByText('3 metas')).toBeInTheDocument();
  });

  it('elegir un estado, un tipo y un plazo avisa el filtro nuevo', async () => {
    const { onCambiar } = pintar();
    await userEvent.click(screen.getByRole('button', { name: 'Todas' }));
    expect(onCambiar).toHaveBeenLastCalledWith({ estado: 'TODAS', tipo: '', plazo: '' });

    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'ACADEMICA');
    expect(onCambiar).toHaveBeenLastCalledWith({ estado: 'ACTIVA', tipo: 'ACADEMICA', plazo: '' });

    await userEvent.selectOptions(screen.getByLabelText('Plazo'), 'CORTO');
    expect(onCambiar).toHaveBeenLastCalledWith({ estado: 'ACTIVA', tipo: '', plazo: 'CORTO' });
  });

  it('con filtros elegidos aparece «Limpiar filtros» y lo avisa', async () => {
    const { onLimpiar } = pintar({ estado: 'CUMPLIDA', tipo: 'OTRO', plazo: '' });
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(onLimpiar).toHaveBeenCalledTimes(1);
  });

  it('mientras carga anuncia la búsqueda en vez del total', () => {
    pintar(FILTROS_POR_DEFECTO, { cargando: true });
    expect(screen.getByText('Buscando metas...')).toBeInTheDocument();
  });
});
