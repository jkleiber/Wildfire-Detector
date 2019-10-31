#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"

#include "BluefruitConfig.h"

/*=========================================================================
    APPLICATION SETTINGS

      FACTORYRESET_ENABLE     Perform a factory reset when running this sketch
     
                              Enabling this will put your Bluefruit LE module
                              in a 'known good' state and clear any config
                              data set in previous sketches or projects, so
                              running this at least once is a good idea.
     
                              When deploying your project, however, you will
                              want to disable factory reset by setting this
                              value to 0.  If you are making changes to your
                              Bluefruit LE device via AT commands, and those
                              changes aren't persisting across resets, this
                              is the reason why.  Factory reset will erase
                              the non-volatile memory where config data is
                              stored, setting it back to factory default
                              values.
         
                              Some sketches that require you to bond to a
                              central device (HID mouse, keyboard, etc.)
                              won't work at all with this feature enabled
                              since the factory reset will clear all of the
                              bonding data stored on the chip, meaning the
                              central device won't be able to reconnect.
    MINIMUM_FIRMWARE_VERSION  Minimum firmware version to have some new features
    MODE_LED_BEHAVIOUR        LED activity, valid options are
                              "DISABLE" or "MODE" or "BLEUART" or
                              "HWUART"  or "SPI"  or "MANUAL"
    -----------------------------------------------------------------------*/
#define FACTORYRESET_ENABLE         1
#define MINIMUM_FIRMWARE_VERSION    "0.6.6"
#define MODE_LED_BEHAVIOUR          "MODE"
#define FIRE_ANALOG                 A0
#define FIRE_DIGITAL                2
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
  pinMode(FIRE_ANALOG, INPUT);
  pinMode(FIRE_DIGITAL, INPUT);
  attachInterrupt(digitalPinToInterrupt(FIRE_DIGITAL), detected, RISING);
}

void loop(void)
{
  // Testing
  delay(1000);
  ble.println("AT+GATTCHAR=1,1");
  ble.waitForOK();
  delay(1000);
  ble.println("AT+GATTCHAR=1,0");
  ble.waitForOK();

  /* Actual logic here
  if(analogRead(FIRE_ANALOG) > 850)
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
  */
}
