package faang.school.accountservice.service;

import faang.school.accountservice.enums.AccountType;
import faang.school.accountservice.exception.AccountGenerationException;
import faang.school.accountservice.exception.InvalidAccountTypeException;
import faang.school.accountservice.exception.NoFreeAccountNumbersException;
import faang.school.accountservice.model.AccountSeq;
import faang.school.accountservice.model.FreeAccountId;
import faang.school.accountservice.model.FreeAccountNumber;
import faang.school.accountservice.repository.AccountNumbersSequenceRepository;
import faang.school.accountservice.repository.FreeAccountNumbersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeAccountNumbersService {

    private static final Map<AccountType, Long> ACCOUNT_PREFIXES = Map.of(
            AccountType.PERSONAL, 4200_0000_0000_0000L,
            AccountType.BUSINESS, 5200_0000_0000_0000L,
            AccountType.CURRENCY, 6300_0000_0000_0000L,
            AccountType.SAVINGS, 7400_0000_0000_0000L
    );

    private final FreeAccountNumbersRepository freeAccountNumbersRepository;
    private final AccountNumbersSequenceRepository accountNumbersSequenceRepository;

    @Transactional
    public void generateFreeAccountNumbers(AccountType accountType, int batchSize) {
        try {
            long prefix = ACCOUNT_PREFIXES.getOrDefault(accountType, 0L);
            if (prefix == 0L) {
                throw new InvalidAccountTypeException("Неизвестный тип счета: " + accountType);
            }

            log.info("Генерация {} свободных номеров счетов для типа счета: {}", batchSize, accountType);

            accountNumbersSequenceRepository.incrementCounter(accountType, batchSize);

            List<FreeAccountNumber> numbers = new ArrayList<>();
            for (long i = 0; i < batchSize; i++) {
                numbers.add(new FreeAccountNumber(new FreeAccountId(accountType, prefix + i)));
            }
            freeAccountNumbersRepository.saveAll(numbers);

            log.info("Успешно сгенерировано {} свободных номеров счетов для типа счета: {}", batchSize, accountType);
        } catch (InvalidAccountTypeException e) {
            log.error("Ошибка при генерации свободных номеров счетов для типа счета: {} - {}", accountType, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при генерации свободных номеров счетов для типа счета: {} - {}", accountType, e.getMessage());
            throw new AccountGenerationException("Ошибка при генерации свободных номеров счетов", e);
        }
    }

    @Transactional
    public void retrieveFreeAccountNumber(AccountType accountType, Consumer<FreeAccountNumber> numberConsumer) {
        try {
            log.info("Получение свободного номера счета для типа счета: {}", accountType);

            FreeAccountNumber freeAccountNumber = freeAccountNumbersRepository.findFirstFreeAccountNumber(accountType.name());

            if (freeAccountNumber == null) {
                log.warn("Нет доступных свободных номеров счетов для типа счета: {}", accountType);
                throw new NoFreeAccountNumbersException("Нет доступных свободных номеров для типа счета: " + accountType);
            }

            freeAccountNumbersRepository.deleteByAccountTypeAndAccountNumber(accountType.name(),
                    freeAccountNumber.getFreeAccountId().getAccountNumber());

            numberConsumer.accept(freeAccountNumber);

            AccountSeq accountSeq = accountNumbersSequenceRepository.findByAccountType(accountType);
            if (accountSeq == null) {
                log.info("Не найден счетчик для типа счета: {}, создаем новый.", accountType);
                accountSeq = new AccountSeq();
                accountSeq.setAccountType(accountType);
                accountSeq.setCounter(0);
                accountNumbersSequenceRepository.save(accountSeq);
            }

            accountSeq.setCounter(accountSeq.getCounter() + 1);
            accountNumbersSequenceRepository.save(accountSeq);

            log.info("Успешно получен свободный номер счета для типа счета: {}", accountType);
        } catch (NoFreeAccountNumbersException e) {
            log.error("Ошибка при получении свободного номера счета для типа счета: {} - {}", accountType, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении свободного номера счета для типа счета: {} - {}", accountType, e.getMessage());
            throw new RuntimeException("Ошибка при получении свободного номера счета", e);
        }
    }

}