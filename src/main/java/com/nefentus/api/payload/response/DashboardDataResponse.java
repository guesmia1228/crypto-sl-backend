package com.nefentus.api.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDataResponse {

    public Long allClicks;
    public double allClicksPercentage;

    public Long signUps;
    public double signUpsPercentage;

    public Long income;
    public double incomePercentage;


}
