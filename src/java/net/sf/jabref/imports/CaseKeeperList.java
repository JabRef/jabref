/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package net.sf.jabref.imports;

/**
 * Container class for lists with keywords where the case should be kept independent of bibstyle
 * 
 */
public class CaseKeeperList {
   
    // Common words in IEEE Xplore that should always be in the given case
   public String[] wordListIEEEXplore = new String[]{
	"VLSI",
	"FPGA",
	"ASIC",
	"ADC",
	"DSP",
	"DAC",
	"RF",
	"FFT",
	"DFT",
	"FIR",
	"IIR",
	"RAM",
	"ROM",
	"CMOS",
	"D/A",
	"A/D",
	"I/Q",
        "Fourier",
        "Winograd",
        "IDCT",
        "DCT",
        "IEEE",
        "SFDR",
        "GSM",
        "WCDMA",
        "CDMA",
        "LTE",
        "EDGE",
        "SNR",
        "MIMO",
        "kHz",
        "MHz",
        "GHz",
        "Nyquist",
        "ADSL",
        "HDTV",
        "VDSL",
        "xDSL",
        "CORDIC",
        "H.264",
        "DWT",
        "Hadamard",
        "MPEG",
        "JPEG",
        "DST",
        "AVC",
        "VHDL",
        "Farrow",
        "Shannon",
        "IC",
        "SQNR",
        "SNDR",
        "ENOB",
        "MS",
        "GS",
        "kS",
        "QPSK",
        "QAM",
        "BPSK",
        "FM",
        "AM",
        "OSR",
        "SAR",
        "Vdd",
        "VDD",
        "VGA",
        "DVI",
        "HDMI",
        "CPU",
        "MCU",
        "IBM",
        "Altera",
        "Xilinx",
        "SoC",
        "RISC",
        "IQ",
        "GPGPU",
        "RNS",
        "OFDM",
        "LDPC",
        "MISO",
        "BER",
        "FER",
        "SAW",
        "GPRS",
        "PAPR",
        "mW",
        "WLAN",
        "WiMAX",
        "Viterbi",
        "SISO",
        "MMSE",
        "SIMO",
        "MAP",
        "HARQ",
        "ARQ",
        "RLS",
        "Verilog",
        "Verilog-A",
        "Verilog-AMS",
        "MOSFET",
        "FET",
        "MOS",
        "LCD",
        "BJT",
        "ANSI",
        "MASH",
        "QoS",
        "PowerPC",
        "LAN",
        "ATM",
        "MAC",
        "WWW",
        "API",
        "UMTS",
        "TDMA",
        "DMT",
        "ISI",
        "GaAs",
        "SiGe",
        "AlGaAs",
        "CBR",
        "VBR",
        "CSIT",
        "CSI",
        "IFFT",
        "IDFT",
        "Remez",
        "WDF",
        "Hilbert",
        "Kalman",
        "3-D",
        "2-D",
        "1-D",
        "MEMS",
        "Monte",
        "Carlo",
        "DVFS"
    };
    
   
   // List of all keyword lists
   private String[][] allLists = new String[][] {
       wordListIEEEXplore
   };
   
   public CaseKeeperList () {
   
   };
   /* Return all lists concatenated
    * Can be done faster once deciding on Java 6.0
    * see: http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
    */
    public String[] getAll() {
        int lengh = 0;
        for (String[] array : allLists) {
            lengh += array.length;
        }
        String[] result = new String[lengh];
        int pos = 0;
        for (String[] array : allLists) {
            for (String element : array) {
                result[pos] = element;
                pos++;
            }
        }
        return result;
    }
}
