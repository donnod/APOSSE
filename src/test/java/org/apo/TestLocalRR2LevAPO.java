/** * Copyright (C) 2016 Tarik Moataz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//***********************************************************************************************//
// This file is to test the 2Lev construction by Cash et al. NDSS'14. 
//**********************************************************************************************

package org.crypto.sse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;

public class TestLocalRR2LevAPO {

	public static void main(String[] args) throws Exception {

		long startTime = System.nanoTime();

		BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));

		//System.out.println("Enter your password :");

		//String pass = keyRead.readLine();
		String pass = "asdf";

		byte[] sk = RR2Lev.keyGen(256, pass, "salt/salt", 100000);

		//System.out.println("Enter the relative path name of the folder that contains the files to make searchable");

		//String pathName = keyRead.readLine();
		String pathName = "/home/donnod/Research/dataset/enron_mail_20150507_extracted";


		ArrayList<File> listOfFile = new ArrayList<File>();
		TextProc.listf(pathName, listOfFile);

		TextProc.TextProc(false, pathName);

		Multimap<String, String> topk = APOModules.getTopCommonKeywods(TextExtractPar.lp1, 500);

		// The two parameters depend on the size of the dataset. Change
		// accordingly to have better search performance
		int bigBlock = 1000;
		int smallBlock = 100;
		int dataSize = 10000;

		// Construction of the global multi-map
		System.out.println("\nBeginning of Encrypted Multi-map creation \n");

		RR2Lev twolev = RR2Lev.constructEMMParGMM(sk, APOModules.obfuscateKeywordLists(topk, listOfFile), bigBlock, smallBlock, dataSize);

		long endTime = System.nanoTime();

		System.out.println("Index build time " + (endTime - startTime));

		String shardsPathName = "/home/donnod/Research/dataset/apoShards";
		FileUtils.cleanDirectory(new File(shardsPathName));
		APOModules.erasureCodeEncoding(listOfFile, pathName, shardsPathName);

		long erasureCodingTime = System.nanoTime();

		System.out.println("Erasure coding time " + (erasureCodingTime - endTime));

		String queryPathName = "/home/donnod/Research/dataset/apoQueryResults";

		while (true) {

			System.out.println("Enter the keyword to search for:");
			String keyword = keyRead.readLine();
			byte[][] token = RR2Lev.token(sk, keyword);
            FileUtils.cleanDirectory(new File(queryPathName));
			System.out.println("Final Result: " + APOModules.erasureCodeDecoding(twolev.query(token, twolev.getDictionary(), twolev.getArray()), shardsPathName, queryPathName));

		}

	}
}
