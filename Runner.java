package runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Runner {

	public static void main(String[] args) throws Exception {

		final Scanner scanner = new Scanner(System.in);

		System.out.println("Csv file to read:");
		final String file = scanner.nextLine();

		System.out.println("Year gap:");
		final int years = scanner.nextInt();

		scanner.close();

		final List<IndexSnapshot> records = new ArrayList<>();

		// Read the file
		try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/portfolios/" + file + ".csv"))) {
			boolean firstLine = true;
			String line;
			while ((line = br.readLine()) != null) {
				if (firstLine) { firstLine = false; continue; } // skip the first line with the name
				final String[] values = line.split(",");
				final String stringedDate = values[0];
				final int year = Integer.valueOf(stringedDate.substring(0, 4));
				final int month = Integer.valueOf(stringedDate.substring(5));
				final LocalDate date = LocalDate.of(year, month, 1); // let's pretend it's the first day of the month
				final int price = BigDecimal.valueOf(Double.valueOf(values[1])).intValue();

				final IndexSnapshot snapshot = new IndexSnapshot(date, price);
				records.add(snapshot);
			}
		}

		// Calculate the percentage difference between the dates
		final List<Integer> percentageDifferences = new ArrayList<>();

		for (final IndexSnapshot snapshot : records) {

			// search the end date (gap=6 ---> 2004->2010)
			final IndexSnapshot next = records.stream().filter(rec -> Objects.equals(rec.date, snapshot.date.plusYears(years))).findFirst().orElse(null);
			if (next == null) { break; }

			final int percentageDifference = (next.price * 100 / snapshot.price) - 100;

			final String sign = percentageDifference > 0 ? "+" : "";
			System.out.println(snapshot.date + " -> " + next.date + " -> " + sign + percentageDifference + "%");
			percentageDifferences.add(percentageDifference);
		}

		;

		final long positivePeriods = percentageDifferences.stream().map(val -> val > 0).count();
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

	private static record IndexSnapshot(LocalDate date, int price) {
	}

}
