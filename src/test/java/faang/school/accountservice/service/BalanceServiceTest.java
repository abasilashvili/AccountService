package faang.school.accountservice.service;

import faang.school.accountservice.dto.BalanceDto;
import faang.school.accountservice.exception.BalanceNotFoundException;
import faang.school.accountservice.mappers.BalanceMapper;
import faang.school.accountservice.model.Balance;
import faang.school.accountservice.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceMapper balanceMapper;

    @InjectMocks
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private BalanceDto createBalanceDto(Long accountId, BigDecimal authBalance, BigDecimal actualBalance) {
        return BalanceDto.builder()
                .id(accountId)
                .accountId(accountId)
                .authBalance(authBalance)
                .actualBalance(actualBalance)
                .build();
    }

    @Test
    void testGetBalanceByAccountId_Success() {
        Long accountId = 1L;
        Balance balance = new Balance();
        BalanceDto balanceDto = createBalanceDto(accountId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        when(balanceRepository.findById(accountId)).thenReturn(Optional.of(balance));
        when(balanceMapper.toDto(balance)).thenReturn(balanceDto);

        BalanceDto result = balanceService.getBalanceByAccountId(accountId);

        assertNotNull(result);
        assertEquals(balanceDto, result);
        verify(balanceRepository, times(1)).findById(accountId);
    }

    @Test
    void testGetBalanceByAccountId_NotFound() {
        Long accountId = 1L;
        when(balanceRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(BalanceNotFoundException.class, () -> balanceService.getBalanceByAccountId(accountId));
        verify(balanceRepository, times(1)).findById(accountId);
    }

    @Test
    void testCreateBalance_Success() {
        Balance balance = new Balance();
        BalanceDto balanceDto = createBalanceDto(null, new BigDecimal("100.00"), new BigDecimal("100.00"));

        when(balanceMapper.toEntity(balanceDto)).thenReturn(balance);
        when(balanceRepository.save(balance)).thenReturn(balance);
        when(balanceMapper.toDto(balance)).thenReturn(balanceDto);

        BalanceDto result = balanceService.createBalance(balanceDto);

        assertNotNull(result);
        assertEquals(balanceDto, result);
        verify(balanceRepository, times(1)).save(balance);
    }

    @Test
    void testUpdateBalance_Success() {
        Long accountId = 1L;
        Balance balance = new Balance();
        BalanceDto balanceDto = createBalanceDto(accountId, new BigDecimal("200.00"), new BigDecimal("150.00"));

        when(balanceRepository.findById(accountId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(balance)).thenReturn(balance);
        when(balanceMapper.toDto(balance)).thenReturn(balanceDto);

        BalanceDto result = balanceService.updateBalance(accountId, balanceDto);

        assertNotNull(result);
        assertEquals(balanceDto, result);
        verify(balanceRepository, times(1)).save(balance);
    }

    @Test
    void testUpdateBalance_NotFound() {
        Long accountId = 1L;
        BalanceDto balanceDto = createBalanceDto(accountId, new BigDecimal("0.00"), new BigDecimal("0.00"));

        when(balanceRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(BalanceNotFoundException.class, () -> balanceService.updateBalance(accountId, balanceDto));
        verify(balanceRepository, times(1)).findById(accountId);
    }
}