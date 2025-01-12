package com.banco.bluebank.service;

import com.banco.bluebank.exceptionhandler.exceptions.*;
import com.banco.bluebank.model.ContatoCliente;
import com.banco.bluebank.model.Correntista;
import com.banco.bluebank.model.Endereco;
import com.banco.bluebank.repository.ContatoClienteRepository;
import com.banco.bluebank.repository.CorrentistaRepository;
import com.banco.bluebank.repository.EnderecoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class CorrentistaService {

    private static final String MSG_CORRENTISTA_EM_USO = "Correntista de id %d não pode ser removido, pois está em uso";
    public static final long CORRENTISTA_BLUEBANK = 1L;

    @Autowired
    private CorrentistaRepository correntistaRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private ContatoClienteRepository contatoClienteRepository;

    @Transactional(readOnly = false)
    public Correntista create(Correntista correntista) {

        if (correntista.getTipoPessoa() != null) correntista.setTipoPessoa(correntista.getTipoPessoa().toUpperCase());

        Correntista novoCorrentista = correntistaRepository.save(correntista);

        List<Endereco> enderecos = correntista.getEnderecos();
        for (Endereco endereco : enderecos) {
            endereco.setIdCorrentista(novoCorrentista.getId());
            endereco = enderecoRepository.save(endereco);
        }

        List<ContatoCliente> contatos = correntista.getContatos();
        for (ContatoCliente contato : contatos) {
            contato.setIdCorrentista(novoCorrentista.getId());
            contato = contatoClienteRepository.save(contato);
        }

        return correntista;
    }

    @Transactional(readOnly = false)
    public Correntista update(Correntista correntista) {

        if (correntista.getId() == CORRENTISTA_BLUEBANK)
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");

        if (correntista.getTipoPessoa() != null) correntista.setTipoPessoa(correntista.getTipoPessoa().toUpperCase());

        return correntistaRepository.save(correntista);

    }

    @Transactional(readOnly = true)
    public Page<Correntista> listar(Pageable pageable) {
        return correntistaRepository.findAll(pageable);
    }


    public Correntista buscar(Long correntistaId) {
        return correntistaRepository.findById(correntistaId)
                .orElseThrow(() -> new CorrentistaNaoEncontradoException(correntistaId));
    }

    @Transactional(readOnly = false)
    public void excluir(Long correntistaId) {

        if (correntistaId == CORRENTISTA_BLUEBANK) {
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");
        }

        buscar(correntistaId);

        try {
            correntistaRepository.deleteById(correntistaId);

        } catch (EmptyResultDataAccessException e) {
            throw new CorrentistaNaoEncontradoException(correntistaId);

        } catch (DataIntegrityViolationException e) {
            throw new EntidadeEmUsoException(
                    String.format(MSG_CORRENTISTA_EM_USO, correntistaId));
        }
    }

    public List<Endereco> listarEnderecosPorCorrentista(Long correntistaId) {
        Correntista correntista = this.buscar(correntistaId);
        return correntista.getEnderecos();
    }

    public List<ContatoCliente> listarContatosPorCorrentista(Long correntistaId) {
        Correntista correntista = this.buscar(correntistaId);
        return correntista.getContatos();
    }

    @Transactional(readOnly = false)
    public List<Endereco> adicionarEndereco(Long correntistaId, Endereco endereco) {

        if (correntistaId == CORRENTISTA_BLUEBANK) {
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");
        }

        Correntista correntista = this.buscar(correntistaId);

        endereco.setIdCorrentista(correntistaId);
        endereco = enderecoRepository.save(endereco);

        return listarEnderecosPorCorrentista(correntistaId);

    }

    @Transactional(readOnly = false)
    public List<ContatoCliente> adicionarContato(Long correntistaId, ContatoCliente contato) {

        if (correntistaId == CORRENTISTA_BLUEBANK) {
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");
        }

        Correntista correntista = this.buscar(correntistaId);

        contato.setIdCorrentista(correntistaId);
        contato = contatoClienteRepository.save(contato);

        return listarContatosPorCorrentista(correntistaId);

    }

    @Transactional(readOnly = false)
    public void excluirEndereco(Long correntistaId, Long enderecoId) {

        if (correntistaId == CORRENTISTA_BLUEBANK) {
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");
        }

        buscar(correntistaId);

        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new EnderecoNaoEncontradoException(enderecoId));

        if (!Objects.equals(endereco.getCorrentista().getId(), correntistaId)) {
            throw new EnderecoNaoEncontradoException(
                    String.format("O endereco especificado %d nao corresponde ao correntista %d", enderecoId, correntistaId));
        }

        try {
            enderecoRepository.deleteById(enderecoId);

        } catch (EmptyResultDataAccessException e) {
            throw new EnderecoNaoEncontradoException(enderecoId);
        }

    }

    @Transactional(readOnly = false)
    public void excluirContato(Long correntistaId, Long contatoId) {

        if (correntistaId == CORRENTISTA_BLUEBANK) {
            throw new RecursoComBloqueioException("Recurso de correntista bloqueado para esta operacão por motivo de manter a integridade da regra de negócios");
        }

        buscar(correntistaId);

        ContatoCliente contato = contatoClienteRepository.findById(contatoId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contatoId));

        if (!Objects.equals(contato.getCorrentista().getId(), correntistaId)) {
            throw new ContatoNaoEncontradoException(
                    String.format("O contato especificado %d nao corresponde ao correntista %d", contatoId, correntistaId));
        }

        try {
            contatoClienteRepository.deleteById(contatoId);

        } catch (EmptyResultDataAccessException e) {
            throw new ContatoNaoEncontradoException(contatoId);
        }
    }
}

