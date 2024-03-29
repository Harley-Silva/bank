package com.harley.bank.service;

import com.harley.bank.model.entities.Transfer;
import com.harley.bank.model.repositories.TransferRepository;
import com.harley.bank.model.services.TransferService;
import com.harley.bank.exceptions.RegraNegocioException;
import com.harley.bank.model.services.implementations.TransferServiceImp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class TransferServiceTest {

    @MockBean
    TransferRepository transferRepository;
    TransferService transferService;

    @BeforeEach
    public void init() {
        this.transferService = new TransferServiceImp(transferRepository);
    }

    @Test
    @DisplayName("Deve salvar um transferência com sucesso.")
    public void givenATransfer_WhenCallCreateTransfer_ThenSaveAndReturnATranfer() {
        LocalDate dateNow = LocalDate.now();
        Transfer transfer = Transfer.builder().originAccount(123456).destinationAccount(654321).transferValue(100.0).transferDate(dateNow).build();
        Transfer expectedTransfer = getTransfer();
        when(transferRepository.save(any(Transfer.class))).thenReturn(expectedTransfer);

        Transfer returnedTransfer = transferService.createTransfer(transfer);

        assertThat(returnedTransfer.getId()).isNotNull();
        assertThat(returnedTransfer.getTransferDate()).isNotNull();
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(100.0);

        verify(transferRepository, times(1)).save(transfer);
    }


    @Test
    @DisplayName("Deve lançar erro ao calcular taxa de transferencia.")
    public void givenInvalidSchedulingDate_WhenCallDescontaTaxa_ThenThrowAnException() {
        LocalDate transferDate = LocalDate.now().minusDays(1);
        Transfer transfer = Transfer.builder().transferDate(transferDate).transferValue(1000.0).build();

        Throwable error = catchThrowableOfType(() -> transferService.createTransfer(transfer), RegraNegocioException.class);
        Assertions.assertThat(error).isInstanceOf(RegraNegocioException.class).hasMessage("A data de transferência deve ser superior a data de hoje.");
        verify(transferRepository, never()).save(transfer);
    }

    @Test
    @DisplayName("Deve lançar erro ao calcular taxa de transferencia.")
    public void givenInvalidSchedulingDateOrTransferValue_WhenCallDescontaTaxa_ThenThrowAnException() {
        LocalDate transferDate = LocalDate.now().plusDays(100);
        Transfer transfer = Transfer.builder().transferDate(transferDate).transferValue(1000.0).build();

        Throwable error = catchThrowableOfType(() -> transferService.createTransfer(transfer), RegraNegocioException.class);
        Assertions.assertThat(error).isInstanceOf(RegraNegocioException.class).hasMessage("Não foi possível calcular uma taxa para os parâmetros passados.");
        verify(transferRepository, never()).save(transfer);
    }

    @Test
    @DisplayName("Deve retorna um page de transfer")
    public void givenAnyParam_WhenCallGetTransfersList_ThenReturnAPageTransfer() {
        Transfer transfer = getTransfer();
        List<Transfer> transferList = Arrays.asList(transfer);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Transfer> transferPage = new PageImpl<>(transferList, pageRequest, 1);
        Mockito.when(transferRepository.findAll(any(Example.class), any(PageRequest.class))).thenReturn(transferPage);

        Page<Transfer> returnedTransfersList = transferService.getTransfersList(transfer, pageRequest);

        assertThat(returnedTransfersList.getTotalElements()).isEqualTo(1);
        assertThat(returnedTransfersList.getContent()).isEqualTo(transferList);
        assertThat(returnedTransfersList.getPageable().getPageNumber()).isZero();
        assertThat(returnedTransfersList.getPageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Dias = 0 e Valor < 1000")
    public void givenCaseA_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now()).transferValue(100.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(94.0);
    }

    @Test
    @DisplayName("Dias > 0 e Valor > 1000")
    public void givenCaseB_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now().plusDays(1)).transferValue(2000.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(1988);
    }

    @Test
    @DisplayName("Dias > 10 e Valor > 2000")
    public void givenCaseC1_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now().plusDays(11)).transferValue(10000.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(9180);
    }

    @Test
    @DisplayName("Dias > 20 e Valor > 2000")
    public void givenCaseC2_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now().plusDays(21)).transferValue(10000.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(9310);
    }


    @Test
    @DisplayName("Dias > 30 e Valor > 2000")
    public void givenCaseC3_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now().plusDays(31)).transferValue(10000.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(9530);
    }

    @Test
    @DisplayName("Dias > 40 e Valor > 2000")
    public void givenCaseC4_WhenCallCalculaTaxa_ThenReturnTransferWithValor() {
        Transfer transfer = Transfer.builder().transferDate(LocalDate.now().plusDays(41)).transferValue(10000.0).build();
        Transfer returnedTransfer = transferService.descontaTaxa(transfer);
        assertThat(returnedTransfer.getTransferValue()).isEqualTo(9830);
    }

    private Transfer getTransfer() {
        LocalDate dateNow = LocalDate.now();

        return Transfer.builder().id(1L).originAccount(123456).transferValue(100.0).destinationAccount(654321).transferDate(dateNow).schedulingDate(dateNow).build();
    }
}
