/** * Copyright (C) 2017 Guoxing Chen
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

// This file contains APOModules
//***********************************************************************************************//

package org.apo.sse;

import com.backblaze.erasure.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class APOModules {
    private static int m = 8;
    private static int k = 2;
    private static double p = 0.895328521728516;
	private static double q = 0.0210561086316476;
    public static final int BYTES_IN_INT = 4;

	public static void setParameters(int m, int k, double p, double q) {
		APOModules.m = m;
		APOModules.k = k;
		APOModules.p = p;
		APOModules.q = q;

	}

	public static Multimap<String, String> getTopCommonKeywods(Multimap<String, String> originalKeywordLists, int k) {
        Multimap<String, String> topCommonKeywords = ArrayListMultimap.create();
	    Map<String, Integer> map = new TreeMap<String, Integer>();
	    for (String key : originalKeywordLists.keySet()) {
	        map.put(key, originalKeywordLists.get(key).size());
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {

            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }

        });
        for (Map.Entry<String, Integer> entry : list) {
            topCommonKeywords.putAll(entry.getKey(), originalKeywordLists.get(entry.getKey()));
            if (k-- == 0) break;
        }
        return topCommonKeywords;
    }

	public static Multimap<String, String> obfuscateKeywordLists(Multimap<String, String> keywordLists, ArrayList<File> listOfFile) {
		Multimap<String, String> obfuscatedKeywordLists = HashMultimap.create();
		Random rng = new Random();

		List<String> listOfKeyword = new ArrayList<String>(keywordLists.keySet());
		for (String keyword : listOfKeyword) {
			for (File file : listOfFile) {
				if (keywordLists.get(keyword).contains(file.getName())) {
					for (int i = 0; i < m; i++) {
						if (rng.nextDouble() < p) {
							obfuscatedKeywordLists.put(keyword, file.getName() + "." + i);
						}
					}
				} else {
					for (int i = 0; i < m; i++) {
						if (rng.nextDouble() < q) {
							obfuscatedKeywordLists.put(keyword, file.getName() + "." + i);
						}
					}
				}
			}
		}
		return obfuscatedKeywordLists;
	}

	public static void erasureCodeEncoding(List<File> listOfFile, String originalPathName, String shardsPathName) throws IOException {
	    for (File file : listOfFile) {
	        encodeOneFile(file, originalPathName, shardsPathName);
        }

    }

	public static List<String> erasureCodeDecoding(List<String> listOfFile, String shardsPathName, String resultsPathName) throws IOException {
        List<String> listOfDecodedFile = new ArrayList<String>();

        Multimap<String, Byte> shardMap = HashMultimap.create();

        for (String fileName : listOfFile) {
            int i = fileName.lastIndexOf('.');
            shardMap.put(fileName.substring(0, i), Byte.valueOf(fileName.substring(i + 1)));

        }

        for (String fileName : shardMap.keySet()) {
            if (shardMap.get(fileName).size() >= k) {
                listOfDecodedFile.add(fileName + shardMap.get(fileName));
                decodeOneFile(fileName, shardMap.get(fileName), shardsPathName, resultsPathName);
            }
        }

        return listOfDecodedFile;
    }

    public static void encodeOneFile(File inputFile, String originalPathName, String shardsPathName) throws IOException {
	    int DATA_SHARDS = k;
	    int PARITY_SHARDS = m - k;
	    int TOTAL_SHARDS = m;

        final int fileSize = (int) inputFile.length();

        // Figure out how big each shard will be.  The total size stored
        // will be the file size (8 bytes) plus the file.
        final int storedSize = fileSize + BYTES_IN_INT;
        final int shardSize = (storedSize + DATA_SHARDS - 1) / DATA_SHARDS;

        // Create a buffer holding the file size, followed by
        // the contents of the file.
        final int bufferSize = shardSize * DATA_SHARDS;
        final byte [] allBytes = new byte[bufferSize];
        ByteBuffer.wrap(allBytes).putInt(fileSize);
        InputStream in = new FileInputStream(inputFile);
        int bytesRead = in.read(allBytes, BYTES_IN_INT, fileSize);
        if (bytesRead != fileSize) {
            throw new IOException("not enough bytes read");
        }
        in.close();

        // Make the buffers to hold the shards.
        byte [] [] shards = new byte [TOTAL_SHARDS] [shardSize];

        // Fill in the data shards
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);

        // Write out the resulting files.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            File outputFile = new File(shardsPathName + inputFile.getAbsolutePath().substring(originalPathName.length()) + "." + i);
            outputFile.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(outputFile);
            out.write(shards[i]);
            out.close();
        }
    }

    public static void decodeOneFile(String inputFile, Collection<Byte> shardIndexes, String shardsPathName, String resultsPathName) throws IOException {
        int DATA_SHARDS = k;
        int PARITY_SHARDS = m - k;
        int TOTAL_SHARDS = m;

        // Read in any of the shards that are present.
        // (There should be checking here to make sure the input
        // shards are the same size, but there isn't.)
        final byte [] [] shards = new byte [TOTAL_SHARDS] [];
        final boolean [] shardPresent = new boolean [TOTAL_SHARDS];
        int shardSize = 0;
        int shardCount = 0;
        for (Byte i : shardIndexes) {
            File shardFile = new File(
                    shardsPathName,
                    inputFile + "." + i);
            if (shardFile.exists()) {
                shardSize = (int) shardFile.length();
                shards[i] = new byte [shardSize];
                shardPresent[i] = true;
                shardCount += 1;
                InputStream in = new FileInputStream(shardFile);
                in.read(shards[i], 0, shardSize);
                in.close();
            }
        }

        // We need at least DATA_SHARDS to be able to reconstruct the file.
        if (shardCount < DATA_SHARDS) {
            System.out.println("Not enough shards present for " + inputFile);
            return;
        }

        // Make empty buffers for the missing shards.
        for (int i = 0; i < TOTAL_SHARDS; i++) {
            if (!shardPresent[i]) {
                shards[i] = new byte [shardSize];
            }
        }

        // Use Reed-Solomon to fill in the missing shards
        ReedSolomon reedSolomon = ReedSolomon.create(DATA_SHARDS, PARITY_SHARDS);
        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        // Combine the data shards into one buffer for convenience.
        // (This is not efficient, but it is convenient.)
        byte [] allBytes = new byte [shardSize * DATA_SHARDS];
        for (int i = 0; i < DATA_SHARDS; i++) {
            System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
        }

        // Extract the file length
        int fileSize = ByteBuffer.wrap(allBytes).getInt();

        // Write the decoded file
        File decodedFile = new File(resultsPathName, inputFile);
        OutputStream out = new FileOutputStream(decodedFile);
        out.write(allBytes, BYTES_IN_INT, fileSize);
    }
}
