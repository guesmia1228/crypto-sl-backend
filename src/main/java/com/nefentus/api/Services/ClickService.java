package com.nefentus.api.Services;

import com.nefentus.api.Errors.UserNotFoundException;
import com.nefentus.api.entities.LinkCounter;
import com.nefentus.api.payload.response.DashboardNumberResponse;
import com.nefentus.api.repositories.AffiliateCounterRepository;
import com.nefentus.api.repositories.LinkCounterRepository;
import com.nefentus.api.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ClickService {
    private AffiliateCounterRepository clickRepository;
    private LinkCounterRepository linkCounterRepository;
    private UserRepository userRepository;

    public DashboardNumberResponse calculateTotalClicks() {
        long totalClicks = linkCounterRepository.count();
        long lastMonthClicks = linkCounterRepository.countByTimestampAfter(Timestamp.valueOf(LocalDateTime.now().minusDays(30)));
        long last30 = totalClicks - lastMonthClicks;
        double percentageIncrease = last30 == 0 ? totalClicks * 100 : ((double) (lastMonthClicks - last30) / last30) * 100;
        DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
        totalClicksDto.setNumber(totalClicks);
        totalClicksDto.setPercentage(percentageIncrease);
        return totalClicksDto;
    }

    public DashboardNumberResponse calculateTotalClicks(String email) {
        long totalClicks = linkCounterRepository.countByUserEmail(email);
        long lastMonthClicks = linkCounterRepository.countByTimestampAfterAndUserEmail(Timestamp.valueOf(LocalDateTime.now().minusDays(30)), email);
        long last30 = totalClicks - lastMonthClicks;
        double percentageIncrease = last30 == 0 ? totalClicks * 100 : ((double) (lastMonthClicks - last30) / last30) * 100;
        DashboardNumberResponse totalClicksDto = new DashboardNumberResponse();
        totalClicksDto.setNumber(totalClicks);
        totalClicksDto.setPercentage(percentageIncrease);
        return totalClicksDto;
    }

    public void addClick(String afflink) throws UserNotFoundException {
        var optUser = userRepository.findByAffiliateLink(afflink);
        if (optUser.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }
        var user = optUser.get();
        LinkCounter click = new LinkCounter();
        click.setUser(user);
        click.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
        linkCounterRepository.save(click);
    }
}
