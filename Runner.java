package runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Runner {

	public static void main(String[] args) throws Exception {

		// Find all csv
		final File[] csvs = new File(System.getProperty("user.dir") + "/portfolios").listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".csv"));
		if (csvs.length == 0) {
			System.out.println("Your 'portfolios' subfolder is empty!");
			System.exit(0);
		}

		// Ask the info
		final Scanner scanner = new Scanner(System.in);
		System.out.println("Choose the portfolio:");

		final Map<Integer, File> csvMap = new HashMap<>();
		int csvIndex = 1;
		for (final File csv : csvs) {
			final int index = csvIndex++;
			csvMap.put(index, csv);
			System.out.println(" -%s %s".formatted(index, csv.getName()));
		}

		int fileIndex = scanner.nextInt();

		while (!csvMap.containsKey(fileIndex)) {
			System.out.println("Number not valid... Try again");
			fileIndex = scanner.nextInt();
		}

		final File file = csvMap.get(fileIndex);
		System.out.println("Chosen: " + file.getName());

		System.out.println("Year gap:");
		final int years = scanner.nextInt();

		scanner.close();

		final List<FileRow> rows = new ArrayList<>();

		// Read the file
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			boolean firstLine = true;
			String line;
			while ((line = br.readLine()) != null) {
				if (firstLine) { firstLine = false; continue; } // skip the first line with the name
				final String[] values = line.split(",");
				final String stringedDate = values[0];
				int year = 0;
				int month = 0;
				if (Objects.equals(stringedDate.indexOf("/"), 2)) {
					year = Integer.valueOf(stringedDate.substring(3));
					month = Integer.valueOf(stringedDate.substring(0, 2));
				} else {
					year = Integer.valueOf(stringedDate.substring(0, 4));
					month = Integer.valueOf(stringedDate.substring(5));
				}
				final LocalDate date = LocalDate.of(year, month, 1);
				final int price = BigDecimal.valueOf(Double.valueOf(values[1])).intValue();

				rows.add(new FileRow(date, price));
			}
		}

		// Calculate the percentage difference between the dates
		final List<Integer> percentageDifferences = new ArrayList<>();

		for (final FileRow row : rows) {

			// search the end date (gap=6 ---> 2004->2010)
			FileRow next = rows.stream().filter(rec -> Objects.equals(rec.date, row.date.plusYears(years))).findFirst().orElse(null);
			if (next == null) {
				if (percentageDifferences.isEmpty()) {
					next = rows.get(rows.size() - 1);
				} else {
					break;
				}
			}

			final int percentageDifference = (next.price * 100 / row.price) - 100;

			final String sign = percentageDifference > 0 ? "+" : "";
			System.out.println(row.stringDate() + " -> " + next.stringDate() + " ---> " + sign + percentageDifference + "%");
			percentageDifferences.add(percentageDifference);
		}

		final long positivePeriods = percentageDifferences.stream().filter(val -> val > 0).count();
		final int average = (int) percentageDifferences.stream().mapToInt(a -> a).average().getAsDouble();
		final int standardDeviation = standardDeviation(percentageDifferences);

		System.out.println("min: " + Collections.min(percentageDifferences) + "%");
		System.out.println("max: " + Collections.max(percentageDifferences) + "%");
		System.out.println("average: " + average + "%");
		System.out.println("median: " + median(percentageDifferences) + "%");
		System.out.println("standard deviation: " + standardDeviation);
		System.out.println("most lays between %s and %s".formatted(average - standardDeviation, average + standardDeviation) + " %");
		System.out.println("positive periods: %s/%s -> %s".formatted(//
				positivePeriods, //
				percentageDifferences.size(), //
				(positivePeriods * 100 / percentageDifferences.size())) + "%");
	}

	/** Calculates the median of the numbers in the list. */
	private static int median(List<Integer> list) {
		if (list.size() == 1) { return list.get(0); }
		list.sort(Comparator.naturalOrder());

		final List<Integer> positions = new ArrayList<>();

		// try to find the middle of the list
		final int temp = list.size() / 2;
		positions.add(temp + 1);

		// if the list size is even I need to calculate the average between the 2 values in the middle
		if (list.size() % 2 == 0) { positions.add(temp); }

		int sum = 0;
		for (final int position : positions) {
			sum += list.get(position);
		}
		return sum / positions.size();
	}

	/** Calculates the standard deviation of the numbers in the list. */
	private static int standardDeviation(List<Integer> list) {

		int sum = 0;
		for (final int num : list) {
			sum = sum += num;
		}

		final double average = sum / list.size();

		double standardDeviation = 0.0;
		for (final Integer num : list) {
			standardDeviation += Math.pow(num.doubleValue() - average, 2);
		}

		return (int) Math.sqrt(standardDeviation / list.size());
	}

	private static record FileRow(LocalDate date, int price) {
		public String stringDate() {
			String month = String.valueOf(date.getMonthValue());
			if (month.length() == 1) { month = "0" + month; }
			return date.getYear() + "-" + month;
		}
	}

}
