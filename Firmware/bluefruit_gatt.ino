#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"

#include "BluefruitConfig.h"
/*    -----------------------------------------------------------------------*/
#define FACTORYRESET_ENABLE         1
#define MINIMUM_FIRMWARE_VERSION    "0.6.6"
#define MODE_LED_BEHAVIOUR          "MODE"
#define FIRE_ANALOG_RIGHT           A1
#define FIRE_DIGITAL_RIGHT          3
#define FIRE_ANALOG_LEFT            A0
#define FIRE_DIGITAL_LEFT           2
#define TEST_LED                    13
/*=========================================================================*/

/* Hardware SPI, using SCK/MOSI/MISO hardware SPI pins and then user selected CS/IRQ/RST */
Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

bool fire;

void detected()
{
  static unsigned long lastInterruptTime = 0;
  if ((millis() - lastInterruptTime > 5000) || (millis() - lastInterruptTime < 0))
  {
    fire = true;
    lastInterruptTime = millis();
  }
}

/**************************************************************************/
/*!¬
    @brief Sets up BLE module and signal inputs·
*/
/**************************************************************************/
void setup(void)
{
  Serial.begin(9600);
  ble.begin(VERBOSE_MODE);
  if ( FACTORYRESET_ENABLE )
  {
    /* Perform a factory reset to make sure everything is in a known state */
    ble.factoryReset();
  }
  /* Disable command echo from Bluefruit */
  ble.echo(false);

  ble.println("AT+GATTCLEAR");
  ble.println("AT+GATTADDSERVICE=UUID128=00-11-00-11-44-55-66-77-88-99-AA-BB-CC-DD-EE-FF");
  ble.println("AT+GATTADDCHAR=UUID=0x0002,PROPERTIES=0x10,MIN_LEN=1,MAX_LEN=1,VALUE=0");
  ble.println("ATZ");

  // Set up digital interrupt
  fire = false;
  pinMode(TEST_LED, OUTPUT);
  pinMode(FIRE_ANALOG_LEFT, INPUT);
  pinMode(FIRE_DIGITAL_LEFT, INPUT);
  pinMode(FIRE_ANALOG_RIGHT, INPUT);
  pinMode(FIRE_DIGITAL_RIGHT, INPUT);
  attachInterrupt(digitalPinToInterrupt(FIRE_DIGITAL_RIGHT), detected, RISING);
  attachInterrupt(digitalPinToInterrupt(FIRE_DIGITAL_LEFT), detected, RISING);

}

void loop(void)
{
  if(analogRead(FIRE_ANALOG_RIGHT) > 900 || analogRead(FIRE_ANALOG_LEFT) > 900)
  {
    detected();
  }

  if (fire)
  {
    ble.println("AT+GATTCHAR=1,1");
    digitalWrite(TEST_LED, HIGH);
    delay(1000);
    ble.println("AT+GATTCHAR=1,0");
    digitalWrite(TEST_LED, LOW);
    fire = false;
  }
  delay(10);
}
