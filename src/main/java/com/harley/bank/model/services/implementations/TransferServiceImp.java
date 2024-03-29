package com.harley.bank.model.services.implementations;

import com.harley.bank.exceptions.RegraNegocioException;
import com.harley.bank.model.entities.Transfer;
import com.harley.bank.model.repositories.TransferRepository;
import com.harley.bank.model.services.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TransferServiceImp implements TransferService {

    private final TransferRepository transferRepository;
    @Override
    @Transactional
    public Transfer createTransfer(Transfer transfer) {
        return transferRepository.save(descontaTaxa(transfer));
    }

    @Override
    public Page<Transfer> getTransfersList(Transfer transfer, Pageable pageable) {
        Example example = Example.of(transfer, ExampleMatcher
                        .matching()
                        .withIgnoreCase()
                        .withIgnoreNullValues()
                        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING));
        return transferRepository.findAll(example, pageable);
    }

    @Override
    public Page<Transfer> getTransfersList(Pageable pageable) {
        return transferRepository.findAll(pageable);
    }

    @Override
    public Transfer descontaTaxa(Transfer transfer) {
        LocalDate todayDate = LocalDate.now();
        LocalDate transferDate = transfer.getTransferDate();

        if (transferDate.isBefore(todayDate)) {
            throw new RegraNegocioException("A data de transferência deve ser superior a data de hoje.");
        }

        transfer.setSchedulingDate(todayDate);

        long days = ChronoUnit.DAYS.between(todayDate, transferDate);
        Double transferValue = transfer.getTransferValue();
        Double taxa;
        if (transferValue > 0.0 && transferValue <= 1000.0 && days == 0) { //CASE A + D
            taxa = (3.0 + (transferValue * 0.030));
        } else if ((transferValue > 1000 && transferValue <= 2000.0) && (days > 0 && days <= 10)) { //CASE B + D
            taxa = 12.0;
        } else if (transferValue > 2000.0) { //CASE C + D
            if (days > 10 && days <= 20) {
                taxa = (transferValue * 0.0820);
            } else if (days > 20 && days <= 30) {
                taxa = (transferValue * 0.0690);
            } else if (days > 30 && days <= 40) {
                taxa = (transferValue * 0.0470);
            } else if (days > 40) {
                taxa = (transferValue * 0.0170);
            } else {
                throw new RegraNegocioException("Não foi possível calcular uma taxa para os parâmetros passados.");
            }
        } else {
            throw new RegraNegocioException("Não foi possível calcular uma taxa para os parâmetros passados.");
        }

        transferValue -= taxa;
        transfer.setTransferTaxa(taxa);
        transfer.setTransferValue(transferValue);

        return transfer;
    }
}
