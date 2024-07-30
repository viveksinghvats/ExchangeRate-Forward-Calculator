# ExchangeRate Forward Calculator

The ExchangeRate Forward Calculator project utilizes the [Interest Rate Parity (IRP)](https://www.investopedia.com/terms/i/interestrateparity.asp#:~:text=Interest%20rate%20parity%20(IRP)%20is,and%20the%20spot%20exchange%20rate.) theory to predict future currency exchange rates, improving decision-making processes for financial planning and analysis.

## Features

- **Accurate Predictions**: Leverages the IRP theory to predict future currency exchange rates with enhanced accuracy.
- **Dynamic Data Fetching**: Developed cron jobs to fetch daily spot rates and Premia values from third-party APIs, with updates occurring on both daily and hourly cycles.
- **Improved Forecast Accuracy**: Achieved a 73% increase in forecast accuracy, based on testing with over 1,000 data points.

## Technologies Used

- **Programming Language**: Java
- **Cloud Services**: AWS SDK, AWS Lambda
- **Database**: DynamoDB
- **Build Tools**: Maven

## Installation

1. **Clone the repository**:
   ```sh
   git clone https://github.com/viveksinghvats/exchangerate-forward-calculator.git
   cd exchangerate-forward-calculator

 2. **Build the project**:
    ```sh
    mvn clean install

