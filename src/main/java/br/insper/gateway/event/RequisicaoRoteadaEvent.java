package br.insper.gateway.event;

import org.springframework.context.ApplicationEvent;

public class RequisicaoRoteadaEvent extends ApplicationEvent {

	private final String servicoDestino;
	private final String path;

	public RequisicaoRoteadaEvent(Object source, String servicoDestino, String path) {
		super(source);
		this.servicoDestino = servicoDestino;
		this.path = path;
	}

	public String getServicoDestino() {
		return servicoDestino;
	}

	public String getPath() {
		return path;
	}
}
