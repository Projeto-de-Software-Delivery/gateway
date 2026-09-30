package br.insper.gateway.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RequisicaoRoteadaListener {

	private static final Logger log = LoggerFactory.getLogger(RequisicaoRoteadaListener.class);

	@EventListener
	public void aoRotear(RequisicaoRoteadaEvent evento) {
		log.info("Requisição roteada para {}: {}", evento.getServicoDestino(), evento.getPath());
	}
}
